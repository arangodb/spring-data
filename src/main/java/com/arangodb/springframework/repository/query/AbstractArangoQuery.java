/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
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

package com.arangodb.springframework.repository.query;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.util.Assert;

import com.arangodb.ArangoCursor;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.core.ArangoOperations;

/**
 * 
 * @author Audrius Malele
 * @author Mark McCormick
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public abstract class AbstractArangoQuery implements RepositoryQuery {

	protected final ArangoQueryMethod method;
	protected final ArangoOperations operations;
	protected final Class<?> domainClass;

	public AbstractArangoQuery(final ArangoQueryMethod method, final ArangoOperations operations) {
		Assert.notNull(method, "ArangoQueryMethod must not be null!");
		Assert.notNull(operations, "ArangoOperations must not be null!");
		this.method = method;
		this.operations = operations;
		this.domainClass = method.getEntityInformation().getJavaType();
	}

	@Override
	public Object execute(final Object[] parameters) {
		final ArangoParameterAccessor accessor = new ArangoParametersParameterAccessor(method, parameters);
		final Map<String, Object> bindVars = new HashMap<>();

		AqlQueryOptions options = mergeQueryOptions(method.getAnnotatedQueryOptions(), accessor.getQueryOptions());
		if (options == null) {
			options = new AqlQueryOptions();
		}

		if (method.isPageQuery()) {
			options.fullCount(true);
		}

		final String query = createQuery(accessor, bindVars, options);

		final ResultProcessor processor = method.getResultProcessor().withDynamicProjection(accessor);
		final Class<?> typeToRead = getTypeToRead(processor);

		final ArangoCursor<?> result = operations.query(query, bindVars, options, typeToRead);
		return processor.processResult(convertResult(result, accessor));
	}

	@Override
	public ArangoQueryMethod getQueryMethod() {
		return method;
	}

	/**
	 * Implementations should create an AQL query with the given
	 * {@link com.arangodb.springframework.repository.query.ArangoParameterAccessor} and set necessary binding
	 * parameters and query options.
	 * 
	 * @param accessor
	 *            provides access to the actual arguments
	 * @param bindVars
	 *            the binding parameter map
	 * @param options
	 *            contains the merged {@link com.arangodb.model.AqlQueryOptions}
	 * @return the created AQL query
	 */
	protected abstract String createQuery(
		ArangoParameterAccessor accessor,
		Map<String, Object> bindVars,
		AqlQueryOptions options);

	protected abstract boolean isCountQuery();

	protected abstract boolean isExistsQuery();

	/**
	 * Merges AqlQueryOptions derived from @QueryOptions with dynamically passed AqlQueryOptions which takes priority
	 * 
	 * @param oldStatic
	 * @param newDynamic
	 * @return
	 */
	protected AqlQueryOptions mergeQueryOptions(final AqlQueryOptions oldStatic, final AqlQueryOptions newDynamic) {
		if (oldStatic == null) {
			return newDynamic;
		}
		if (newDynamic == null) {
			return oldStatic;
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

	private Class<?> getTypeToRead(final ResultProcessor processor) {
		if (isExistsQuery()) {
			return Integer.class;
		}

		if (method.isGeoQuery()) {
			return Map.class;
		}

		final Class<?> typeToRead = processor.getReturnedType().getTypeToRead();
		return typeToRead != null ? typeToRead : Map.class;
	}

	private Object convertResult(final ArangoCursor<?> result, final ArangoParameterAccessor accessor) {
		if (isExistsQuery()) {
			if (!result.hasNext()) {
				return false;
			}
			return (Integer) result.next() > 0;
		}
		final ArangoResultConverter resultConverter = new ArangoResultConverter(accessor, result, operations,
				domainClass);
		return resultConverter.convertResult(method.getReturnType().getType());
	}

}
