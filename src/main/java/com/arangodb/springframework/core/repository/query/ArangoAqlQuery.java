package com.arangodb.springframework.core.repository.query;

import com.arangodb.ArangoCursor;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.annotation.BindVars;
import com.arangodb.springframework.annotation.Param;
import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.repository.query.derived.DerivedQueryCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by F625633 on 12/07/2017.
 */
public class ArangoAqlQuery implements RepositoryQuery {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArangoAqlQuery.class);

	private final ArangoOperations operations;
	private final Class<?> domainClass;
	private final Method method;
	private final RepositoryMetadata metadata;

	public ArangoAqlQuery(Class<?> domainClass, Method method, RepositoryMetadata metadata, ArangoOperations operations) {
		this.domainClass = domainClass;
		this.method = method;
		this.metadata = metadata;
		this.operations = operations;
	}

	@Override public QueryMethod getQueryMethod() {
		return new ArangoQueryMethod(method, metadata);
	}

	@Override public Object execute(Object[] arguments) {
		Map<String, Object> bindVars = new HashMap<>();
		String query = getQueryAnnotationValue();
		AqlQueryOptions options = null;
		boolean isCountProjection = false;
		if (query == null) {
			PartTree tree = new PartTree(method.getName(), domainClass);
			isCountProjection = tree.isCountProjection();
			query = new DerivedQueryCreator((ArangoMappingContext) operations.getConverter().getMappingContext(), domainClass,
					tree, new ParametersParameterAccessor(new ArangoParameters(method), arguments), bindVars).createQuery();
		} else if (arguments != null) {
			String fixedQuery = removeAqlStringLiterals(query);
			Set<String> bindings = getBindings(fixedQuery);
			Annotation[][] annotations = method.getParameterAnnotations();
			Assert.isTrue(arguments.length == annotations.length,
					"arguments.length != annotations.length");
			Map<String, Object> bindVarsLocal = new HashMap<>();
			boolean bindVarsFound = false;
			for (int i = 0; i < arguments.length; ++i) {
				if (arguments[i] instanceof AqlQueryOptions) {
					Assert.isTrue(options == null, "AqlQueryOptions are already set");
					options = (AqlQueryOptions) arguments[i];
					continue;
				}
				String parameter = null;
				Annotation specialAnnotation = getSpecialAnnotation(annotations[i]);
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
					String key = String.format("%d", i);
					if (bindings.contains(key)) {
						Assert.isTrue(!bindVarsLocal.containsKey(key), "duplicate parameter name");
						bindVarsLocal.put(key, arguments[i]);
					} else if (bindings.contains("@" + key)) {
						Assert.isTrue(!bindVarsLocal.containsKey("@" + key), "duplicate parameter name");
						bindVarsLocal.put("@" + key, arguments[i]);
					} else LOGGER.debug("Local parameter '@{}' is not used in the query", key);
				} else {
					Assert.isTrue(!bindVarsLocal.containsKey(parameter), "duplicate parameter name");
					bindVarsLocal.put(parameter, arguments[i]);
				}
			}
			mergeBindVars(bindVars, bindVarsLocal);
		}
		Class<?> resultClass = isCountProjection ? Integer.class : domainClass;
		return convertResult(operations.query(query, bindVars, options, resultClass));
	}

	private Annotation getSpecialAnnotation(Annotation[] annotations) {
		Annotation specialAnnotation = null;
		for (Annotation annotation : annotations) {
			if (annotation.annotationType() == BindVars.class || annotation.annotationType() == Param.class) {
				Assert.isTrue(specialAnnotation == null, "@BindVars or @Param should be used only once per parameter");
				specialAnnotation = annotation;
			}
		}
		return specialAnnotation;
	}

	private String getQueryAnnotationValue() {
		Query query = method.getAnnotation(Query.class);
		return query == null ? null : query.value();
	}

	private void mergeBindVars(Map<String, Object> bindVars, Map<String, Object> bindVarsLocal) {
		for (String key : bindVarsLocal.keySet()) {
			if (bindVars.containsKey(key)) LOGGER.debug("Local parameter '{}' overrides @BindVars Map", key);
			bindVars.put(key, bindVarsLocal.get(key));
		}
	}

	private Object convertResult(ArangoCursor result) {
		if (List.class.isAssignableFrom(method.getReturnType())) { return result.asListRemaining(); }
		else if (Set.class.isAssignableFrom(method.getReturnType())) {
			Set set = new HashSet();
			result.forEachRemaining(set::add);
			return set;
		} else if (Iterable.class.isAssignableFrom(method.getReturnType())) { return result.asListRemaining(); }
		else if (method.getReturnType().isArray()) { return result.asListRemaining().toArray(); }
		return result.hasNext() ? result.next() : null;
	}

	private String removeAqlStringLiterals(String query){
		StringBuilder fixedQuery = new StringBuilder();
		for (int i = 0; i < query.length(); ++i) {
			if (query.charAt(i) == '"') {
				for (++i; i < query.length(); ++i) {
					if (query.charAt(i) == '"') { ++i; break; }
					if (query.charAt(i) == '\\') ++i;
				}
			} else if (query.charAt(i) == '\'') {
				for (++i; i < query.length(); ++i) {
					if (query.charAt(i) == '\'') { ++i; break; }
					if (query.charAt(i) == '\\') ++i;
				}
			}
			fixedQuery.append(query.charAt(i));
		}
		return fixedQuery.toString();
	}

	private Set<String> getBindings(String query) {
		Set<String> bindings = new HashSet<>();
		Matcher matcher = Pattern.compile("@\\S+").matcher(query);
		while (matcher.find()) bindings.add(matcher.group().substring(1));
		return bindings;
	}
}
