/*
 * DISCLAIMER
 *
 * Copyright 2017 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.springframework.core.repository.query;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.geo.GeoPage;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.Assert;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.entity.IndexType;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.annotation.BindVars;
import com.arangodb.springframework.annotation.Param;
import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.annotation.QueryOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.repository.query.derived.DerivedQueryCreator;

/**
 * Implements execute(Object[]) method which is called every time a user-defined AQL or derived method is called
 */
public class ArangoAqlQuery implements RepositoryQuery {

	private static final Set<Class<?>> GEO_RETURN_TYPES = new HashSet<>();
	private static final Set<Class<?>> DESERIALIZABLE_TYPES = new HashSet<>();

	static {
		GEO_RETURN_TYPES.add(GeoResult.class);
		GEO_RETURN_TYPES.add(GeoResults.class);
		GEO_RETURN_TYPES.add(GeoPage.class);

		DESERIALIZABLE_TYPES.add(Map.class);
		DESERIALIZABLE_TYPES.add(BaseDocument.class);
		DESERIALIZABLE_TYPES.add(BaseEdgeDocument.class);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(ArangoAqlQuery.class);

	private final ArangoOperations operations;
	private final Class<?> domainClass;
	private final Method method;
	private final RepositoryMetadata metadata;
	private ArangoParameterAccessor accessor;
	private boolean isCountProjection = false;
	private boolean isExistsProjection = false;
	private final ProjectionFactory factory;

	public ArangoAqlQuery(final Class<?> domainClass, final Method method, final RepositoryMetadata metadata,
		final ArangoOperations operations, final ProjectionFactory factory) {
		this.domainClass = domainClass;
		this.method = method;
		this.metadata = metadata;
		this.operations = operations;
		this.factory = factory;
	}

	@Override
	public QueryMethod getQueryMethod() {
		return new ArangoQueryMethod(method, metadata, factory);
	}

	/**
	 * This method contains main logic showing how all user-defined methods are implemented
	 * 
	 * @param arguments
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object execute(final Object[] arguments) {
		Map<String, Object> bindVars = new HashMap<>();
		String query = getQueryAnnotationValue();
		AqlQueryOptions options = getAqlQueryOptions();
		boolean optionsFound = false;
		if (query == null) { // derived method
			final PartTree tree = new PartTree(method.getName(), domainClass);
			isCountProjection = tree.isCountProjection();
			isExistsProjection = tree.isExistsProjection();
			accessor = new ArangoParameterAccessor(new ArangoParameters(method), arguments);
			options = updateAqlQueryOptions(options, accessor.getAqlQueryOptions());
			if (Page.class.isAssignableFrom(method.getReturnType())) {
				if (options == null) {
					options = new AqlQueryOptions().fullCount(true);
				} else {
					options = options.fullCount(true);
				}
			}
			final List<String> geoFields = new LinkedList<>();
			if (GEO_RETURN_TYPES.contains(method.getReturnType())) {
				operations.collection(
					operations.getConverter().getMappingContext().getPersistentEntity(domainClass).getCollection())
						.getIndexes().forEach(i -> {
							if ((i.getType() == IndexType.geo1) && geoFields.isEmpty()) {
								i.getFields().forEach(f -> geoFields.add(f));
							}
						});
			}
			query = new DerivedQueryCreator((ArangoMappingContext) operations.getConverter().getMappingContext(),
					domainClass, tree, accessor, bindVars, geoFields,
					operations.getVersion().getVersion().compareTo("3.2.0") < 0).createQuery();
		} else if (arguments != null) { // AQL query method
			final Set<String> bindings = getBindings(query);
			final Annotation[][] annotations = method.getParameterAnnotations();
			Assert.isTrue(arguments.length == annotations.length, "arguments.length != annotations.length");
			final Map<String, Object> bindVarsLocal = new HashMap<>();
			boolean bindVarsFound = false;
			for (int i = 0; i < arguments.length; ++i) {
				if (arguments[i] instanceof AqlQueryOptions) {
					Assert.isTrue(!optionsFound, "AqlQueryOptions are already set");
					optionsFound = true;
					options = updateAqlQueryOptions(options, (AqlQueryOptions) arguments[i]);
					continue;
				}
				String parameter = null;
				final Annotation specialAnnotation = getSpecialAnnotation(annotations[i]);
				if (specialAnnotation != null) {
					if (specialAnnotation.annotationType() == Param.class) {
						parameter = ((Param) specialAnnotation).value();
					} else if (specialAnnotation.annotationType() == BindVars.class) {
						Assert.isTrue(arguments[i] instanceof Map, "@BindVars must be a Map");
						Assert.isTrue(!bindVarsFound, "@BindVars duplicated");
						bindVars = (Map<String, Object>) arguments[i];
						bindVarsFound = true;
						continue;
					}
				}
				if (parameter == null) {
					final String key = String.format("%d", i);
					if (bindings.contains(key)) {
						Assert.isTrue(!bindVarsLocal.containsKey(key), "duplicate parameter name");
						bindVarsLocal.put(key, arguments[i]);
					} else if (bindings.contains("@" + key)) {
						Assert.isTrue(!bindVarsLocal.containsKey("@" + key), "duplicate parameter name");
						bindVarsLocal.put("@" + key, arguments[i]);
					} else {
						LOGGER.debug("Local parameter '@{}' is not used in the query", key);
					}
				} else {
					Assert.isTrue(!bindVarsLocal.containsKey(parameter), "duplicate parameter name");
					bindVarsLocal.put(parameter, arguments[i]);
				}
			}
			mergeBindVars(bindVars, bindVarsLocal);
		}
		return convertResult(operations.query(query, bindVars, options, getResultClass()));
	}

	/**
	 * Merges AqlQueryOptions derived from @QueryOptions with dynamically passed AqlQueryOptions which takes priority
	 * 
	 * @param oldStatic
	 * @param newDynamic
	 * @return
	 */
	private AqlQueryOptions updateAqlQueryOptions(final AqlQueryOptions oldStatic, final AqlQueryOptions newDynamic) {
		if (oldStatic == null) {
			return newDynamic;
		}
		final Integer batchSize = newDynamic.getBatchSize();
		if (batchSize != null) {
			oldStatic.batchSize(batchSize);
		}
		final Integer maxPlans = newDynamic.getMaxPlans();
		if (maxPlans != null) {
			oldStatic.maxPlans(maxPlans);
		}
		final Integer ttl = newDynamic.getTtl();
		if (ttl != null) {
			oldStatic.ttl(ttl);
		}
		final Boolean cache = newDynamic.getCache();
		if (cache != null) {
			oldStatic.cache(cache);
		}
		final Boolean count = newDynamic.getCount();
		if (count != null) {
			oldStatic.count(count);
		}
		final Boolean fullCount = newDynamic.getFullCount();
		if (fullCount != null) {
			oldStatic.fullCount(fullCount);
		}
		final Boolean profile = newDynamic.getProfile();
		if (profile != null) {
			oldStatic.profile(profile);
		}
		final Collection<String> rules = newDynamic.getRules();
		if (rules != null) {
			oldStatic.rules(rules);
		}
		return oldStatic;
	}

	private AqlQueryOptions getAqlQueryOptions() {
		final QueryOptions queryOptions = method.getAnnotation(QueryOptions.class);
		if (queryOptions == null) {
			return null;
		}
		final AqlQueryOptions options = new AqlQueryOptions();
		final int batchSize = queryOptions.batchSize();
		if (batchSize != -1) {
			options.batchSize(batchSize);
		}
		final int maxPlans = queryOptions.maxPlans();
		if (maxPlans != -1) {
			options.maxPlans(maxPlans);
		}
		final int ttl = queryOptions.ttl();
		if (ttl != -1) {
			options.ttl(ttl);
		}
		options.cache(queryOptions.cache());
		options.count(queryOptions.count());
		options.fullCount(queryOptions.fullCount());
		options.profile(queryOptions.profile());
		options.rules(Arrays.asList(queryOptions.rules()));
		return options;
	}

	private Class<?> getResultClass() {
		if (isCountProjection || isExistsProjection) {
			return Integer.class;
		}
		if (GEO_RETURN_TYPES.contains(method.getReturnType())) {
			return Object.class;
		}
		if (DESERIALIZABLE_TYPES.contains(method.getReturnType())) {
			return method.getReturnType();
		}
		return domainClass;
	}

	/**
	 * Returns Param or BindVars Annotation if it is present in the given array or null otherwise
	 * 
	 * @param annotations
	 * @return
	 */
	private Annotation getSpecialAnnotation(final Annotation[] annotations) {
		Annotation specialAnnotation = null;
		for (final Annotation annotation : annotations) {
			if (annotation.annotationType() == BindVars.class || annotation.annotationType() == Param.class) {
				Assert.isTrue(specialAnnotation == null, "@BindVars or @Param should be used only once per parameter");
				specialAnnotation = annotation;
			}
		}
		return specialAnnotation;
	}

	private String getQueryAnnotationValue() {
		final Query query = method.getAnnotation(Query.class);
		return query == null ? null : query.value();
	}

	/**
	 * Merges bindVars Map passed by a user with a Map created from the rest of the arguments which take priority
	 * 
	 * @param bindVars
	 * @param bindVarsLocal
	 */
	private void mergeBindVars(final Map<String, Object> bindVars, final Map<String, Object> bindVarsLocal) {
		for (final String key : bindVarsLocal.keySet()) {
			if (bindVars.containsKey(key)) {
				LOGGER.debug("Local parameter '{}' overrides @BindVars Map", key);
			}
			bindVars.put(key, bindVarsLocal.get(key));
		}
	}

	private Object convertResult(final ArangoCursor<?> result) {
		if (isExistsProjection) {
			if (!result.hasNext()) {
				return false;
			}
			return ((int) result.next()) > 0;
		}
		final ArangoResultConverter resultConverter = new ArangoResultConverter(accessor, result, operations,
				domainClass);
		return resultConverter.convertResult(method.getReturnType());
	}

	private String removeAqlStringLiterals(final String query) {
		final StringBuilder fixedQuery = new StringBuilder();
		for (int i = 0; i < query.length(); ++i) {
			if (query.charAt(i) == '"') {
				for (++i; i < query.length(); ++i) {
					if (query.charAt(i) == '"') {
						++i;
						break;
					}
					if (query.charAt(i) == '\\') {
						++i;
					}
				}
			} else if (query.charAt(i) == '\'') {
				for (++i; i < query.length(); ++i) {
					if (query.charAt(i) == '\'') {
						++i;
						break;
					}
					if (query.charAt(i) == '\\') {
						++i;
					}
				}
			}
			fixedQuery.append(query.charAt(i));
		}
		return fixedQuery.toString();
	}

	/**
	 * Returns all bindings used in AQL query String including bindings prefixed with both single and double '@'
	 * character ignoring AQL string literals
	 * 
	 * @param query
	 * @return
	 */
	private Set<String> getBindings(final String query) {
		final String fixedQuery = removeAqlStringLiterals(query);
		final Set<String> bindings = new HashSet<>();
		final Matcher matcher = Pattern.compile("@\\S+").matcher(fixedQuery);
		while (matcher.find()) {
			bindings.add(matcher.group().substring(1));
		}
		return bindings;
	}
}
