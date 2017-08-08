package com.arangodb.springframework.core.repository.query;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.entity.IndexType;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.annotation.BindVars;
import com.arangodb.springframework.annotation.Param;
import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.repository.query.derived.DerivedQueryCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.geo.*;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
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

	private static final Set<Class<?>> RETURN_TYPES_USING_MAP = new HashSet<>();
	private static final Set<Class<?>> GEO_RETURN_TYPES = new HashSet<>();

	static {
		RETURN_TYPES_USING_MAP.add(Map.class);
		RETURN_TYPES_USING_MAP.add(BaseDocument.class);
		RETURN_TYPES_USING_MAP.add(BaseEdgeDocument.class);
		RETURN_TYPES_USING_MAP.add(GeoResult.class);
		RETURN_TYPES_USING_MAP.add(GeoResults.class);
		RETURN_TYPES_USING_MAP.add(GeoPage.class);

		GEO_RETURN_TYPES.add(GeoResult.class);
		GEO_RETURN_TYPES.add(GeoResults.class);
		GEO_RETURN_TYPES.add(GeoPage.class);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(ArangoAqlQuery.class);

	private final ArangoOperations operations;
	private final Class<?> domainClass;
	private final Method method;
	private final RepositoryMetadata metadata;
	private ArangoParameterAccessor accessor;
	private boolean isCountProjection = false;
	private final ProjectionFactory factory;

	public ArangoAqlQuery(Class<?> domainClass, Method method, RepositoryMetadata metadata,
		ArangoOperations operations, ProjectionFactory factory) {
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

	@Override
	public Object execute(Object[] arguments) {
		Map<String, Object> bindVars = new HashMap<>();
		String query = getQueryAnnotationValue();
		AqlQueryOptions options = null;
		if (query == null) {
			PartTree tree = new PartTree(method.getName(), domainClass);
			isCountProjection = tree.isCountProjection();
			accessor = new ArangoParameterAccessor(new ArangoParameters(method), arguments);
			options = accessor.getAqlQueryOptions();
			if (Page.class.isAssignableFrom(method.getReturnType())) {
				if (options == null) { options = new AqlQueryOptions().fullCount(true); }
				else options = options.fullCount(true);
			}
			List<String> geoFields = new LinkedList<>();
			if (GEO_RETURN_TYPES.contains(method.getReturnType())) {
				operations.collection(operations.getConverter().getMappingContext().getPersistentEntity(domainClass).getCollection())
						.getIndexes().forEach(i -> {
							if ((i.getType() == IndexType.geo1 || i.getType() == IndexType.geo2) && geoFields.isEmpty()) {
								i.getFields().forEach(f -> geoFields.add(f));
							}
						});
			}
			query = new DerivedQueryCreator((ArangoMappingContext) operations.getConverter().getMappingContext(),
					domainClass, tree, accessor, bindVars, geoFields, operations.getVersion().getVersion().compareTo("3.2.0") < 0).createQuery();
		} else if (arguments != null) {
			String fixedQuery = removeAqlStringLiterals(query);
			Set<String> bindings = getBindings(fixedQuery);
			Annotation[][] annotations = method.getParameterAnnotations();
			Assert.isTrue(arguments.length == annotations.length, "arguments.length != annotations.length");
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
					} else
						LOGGER.debug("Local parameter '@{}' is not used in the query", key);
				} else {
					Assert.isTrue(!bindVarsLocal.containsKey(parameter), "duplicate parameter name");
					bindVarsLocal.put(parameter, arguments[i]);
				}
			}
			mergeBindVars(bindVars, bindVarsLocal);
		}
		return convertResult(operations.query(query, bindVars, options, getResultClass()));
	}

	private Class<?> getResultClass() {
		if (isCountProjection) return Integer.class;
		if (RETURN_TYPES_USING_MAP.contains(method.getReturnType())) return Object.class;
		return domainClass;
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
			if (bindVars.containsKey(key))
				LOGGER.debug("Local parameter '{}' overrides @BindVars Map", key);
			bindVars.put(key, bindVarsLocal.get(key));
		}
	}

	private Object convertResult(ArangoCursor result) {
		if (!result.hasNext()) return null;
		ArangoResultConverter resultConverter = new ArangoResultConverter(accessor, result, operations, domainClass);
		return resultConverter.convertResult(method.getReturnType());
	}

	private String removeAqlStringLiterals(String query) {
		StringBuilder fixedQuery = new StringBuilder();
		for (int i = 0; i < query.length(); ++i) {
			if (query.charAt(i) == '"') {
				for (++i; i < query.length(); ++i) {
					if (query.charAt(i) == '"') {
						++i;
						break;
					}
					if (query.charAt(i) == '\\')
						++i;
				}
			} else if (query.charAt(i) == '\'') {
				for (++i; i < query.length(); ++i) {
					if (query.charAt(i) == '\'') {
						++i;
						break;
					}
					if (query.charAt(i) == '\\')
						++i;
				}
			}
			fixedQuery.append(query.charAt(i));
		}
		return fixedQuery.toString();
	}

	private Set<String> getBindings(String query) {
		Set<String> bindings = new HashSet<>();
		Matcher matcher = Pattern.compile("@\\S+").matcher(query);
		while (matcher.find())
			bindings.add(matcher.group().substring(1));
		return bindings;
	}
}
