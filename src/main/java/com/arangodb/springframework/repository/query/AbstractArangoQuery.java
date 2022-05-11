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

import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.util.Pair;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractArangoQuery.class);

	protected final ArangoQueryMethod method;
	protected final ArangoOperations operations;
	protected final ArangoMappingContext mappingContext;
	protected final Class<?> domainClass;
	private final QueryTransactionBridge transactionBridge;

	public AbstractArangoQuery(final ArangoQueryMethod method, final ArangoOperations operations,
							   final QueryTransactionBridge transactionBridge) {
		Assert.notNull(method, "ArangoQueryMethod must not be null!");
		Assert.notNull(operations, "ArangoOperations must not be null!");
		Assert.notNull(transactionBridge, "QueryTransactionBridge must not be null!");
		this.method = method;
		this.operations = operations;
		mappingContext = (ArangoMappingContext) operations.getConverter().getMappingContext();
		this.domainClass = method.getEntityInformation().getJavaType();
		this.transactionBridge = transactionBridge;
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

		final Pair<String, ? extends Collection<String>> queryAndCollection = createQuery(accessor, bindVars, options);
		if (options.getStreamTransactionId() == null) {
			options.streamTransactionId(transactionBridge.beginCurrentTransaction(queryAndCollection.getSecond()));
		}


		final ResultProcessor processor = method.getResultProcessor().withDynamicProjection(accessor);
		final Class<?> typeToRead = getTypeToRead(processor);

		final ArangoCursor<?> result = operations.query(queryAndCollection.getFirst(), bindVars, options, typeToRead);
		logWarningsIfNecessary(result);
		return processor.processResult(convertResult(result, accessor));
	}

	private void logWarningsIfNecessary(final ArangoCursor<?> result) {
		result.getWarnings().forEach(warning -> {
			LOGGER.warn("Query warning at [" + method + "]: " + warning.getCode() + " - " + warning.getMessage());
		});
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
	 * @return a pair of the created AQL query and all collection names
	 */
	protected abstract Pair<String, ? extends Collection<String>> createQuery(
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

		AqlQueryOptions mergedOptions = newDynamic.clone();

		if (mergedOptions.getBatchSize() == null) {
			mergedOptions.batchSize(oldStatic.getBatchSize());
		}

		if (mergedOptions.getCache() == null) {
			mergedOptions.cache(oldStatic.getCache());
		}

		if (mergedOptions.getCount() == null) {
			mergedOptions.count(oldStatic.getCount());
		}

		if (mergedOptions.getFullCount() == null) {
			mergedOptions.fullCount(oldStatic.getFullCount());
		}

		if (mergedOptions.getMaxPlans() == null) {
			mergedOptions.maxPlans(oldStatic.getMaxPlans());
		}

		if (mergedOptions.getProfile() == null) {
			mergedOptions.profile(oldStatic.getProfile());
		}
		if (mergedOptions.getRules() == null) {
			mergedOptions.rules(oldStatic.getRules());
		}

		if (mergedOptions.getTtl() == null) {
			mergedOptions.ttl(oldStatic.getTtl());
		}

		if (mergedOptions.getStream() == null) {
			mergedOptions.stream(oldStatic.getStream());
		}

		if (mergedOptions.getMemoryLimit() == null) {
			mergedOptions.memoryLimit(oldStatic.getMemoryLimit());
		}

		if (mergedOptions.getAllowDirtyRead() == null) {
			mergedOptions.allowDirtyRead(oldStatic.getAllowDirtyRead());
		}

		if (mergedOptions.getStreamTransactionId() == null) {
			mergedOptions.streamTransactionId(oldStatic.getStreamTransactionId());
		}
		return mergedOptions;
	}

	private Class<?> getTypeToRead(final ResultProcessor processor) {
		if (isExistsQuery()) {
			return Integer.class;
		}

		if (method.isGeoQuery()) {
			return JsonNode.class;
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
		final ArangoResultConverter<?> resultConverter = new ArangoResultConverter<>(accessor, result, operations, domainClass);
		return resultConverter.convertResult(method.getReturnType().getType());
	}

}
