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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.velocypack.VPackSlice;

/**
 * 
 * @author Andrew Fleming
 * @author Audrius Malele
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public abstract class AbstractArangoQuery implements RepositoryQuery {

	private static final Set<Class<?>> DESERIALIZABLE_TYPES = new HashSet<>();

	static {
		DESERIALIZABLE_TYPES.add(Map.class);
		DESERIALIZABLE_TYPES.add(BaseDocument.class);
		DESERIALIZABLE_TYPES.add(BaseEdgeDocument.class);
		DESERIALIZABLE_TYPES.add(String.class);
		DESERIALIZABLE_TYPES.add(Boolean.class);
		DESERIALIZABLE_TYPES.add(boolean.class);
		DESERIALIZABLE_TYPES.add(Integer.class);
		DESERIALIZABLE_TYPES.add(int.class);
		DESERIALIZABLE_TYPES.add(Long.class);
		DESERIALIZABLE_TYPES.add(long.class);
		DESERIALIZABLE_TYPES.add(Short.class);
		DESERIALIZABLE_TYPES.add(short.class);
		DESERIALIZABLE_TYPES.add(Double.class);
		DESERIALIZABLE_TYPES.add(double.class);
		DESERIALIZABLE_TYPES.add(Float.class);
		DESERIALIZABLE_TYPES.add(float.class);
		DESERIALIZABLE_TYPES.add(BigInteger.class);
		DESERIALIZABLE_TYPES.add(BigDecimal.class);
		DESERIALIZABLE_TYPES.add(Number.class);
		DESERIALIZABLE_TYPES.add(Character.class);
		DESERIALIZABLE_TYPES.add(char.class);
		DESERIALIZABLE_TYPES.add(Date.class);
		DESERIALIZABLE_TYPES.add(java.sql.Date.class);
		DESERIALIZABLE_TYPES.add(java.sql.Timestamp.class);
		DESERIALIZABLE_TYPES.add(VPackSlice.class);
		DESERIALIZABLE_TYPES.add(UUID.class);
		DESERIALIZABLE_TYPES.add(byte[].class);
		DESERIALIZABLE_TYPES.add(Byte.class);
		DESERIALIZABLE_TYPES.add(byte.class);
		DESERIALIZABLE_TYPES.add(Enum.class);
		DESERIALIZABLE_TYPES.add(Instant.class);
		DESERIALIZABLE_TYPES.add(LocalDate.class);
		DESERIALIZABLE_TYPES.add(LocalDateTime.class);
		DESERIALIZABLE_TYPES.add(OffsetDateTime.class);
		DESERIALIZABLE_TYPES.add(ZonedDateTime.class);
	}

	protected final ArangoQueryMethod method;
	protected final ArangoOperations operations;
	protected final Class<?> domainClass;

	public AbstractArangoQuery(ArangoQueryMethod method, ArangoOperations operations) {
		Assert.notNull(method, "ArangoQueryMethod must not be null!");
		Assert.notNull(operations, "ArangoOperations must not be null!");
		this.method = method;
		this.operations = operations;
		this.domainClass = method.getEntityInformation().getJavaType();
	}

	@Override
	public Object execute(Object[] parameters) {
		final ArangoParameterAccessor accessor = new ArangoParametersParameterAccessor(method, parameters);
		final Map<String, Object> bindVars = new HashMap<>();
		AqlQueryOptions options = mergeQueryOptions(method.getAnnotatedQueryOptions(), accessor.getQueryOptions());
		if (options == null) {
			options = new AqlQueryOptions();
		}
		final String query = createQuery(accessor, bindVars, options);
		return convertResult(operations.query(query, bindVars, options, getResultClass()), accessor);
	}

	@Override
	public ArangoQueryMethod getQueryMethod() {
		return method;
	}

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

	private Class<?> getResultClass() {
		if (isCountQuery() || isExistsQuery()) {
			return Integer.class;
		}
		if (method.isGeoQuery()) {
			return Object.class;
		}
		if (DESERIALIZABLE_TYPES.contains(method.getReturnType())) {
			return method.getReturnType();
		}
		return domainClass;
	}

	private Object convertResult(final ArangoCursor<?> result, ArangoParameterAccessor accessor) {
		if (isExistsQuery()) {
			if (!result.hasNext()) {
				return false;
			}
			return Integer.valueOf(result.next().toString()) > 0;
		}
		final ArangoResultConverter resultConverter = new ArangoResultConverter(accessor, result, operations,
				domainClass);
		return resultConverter.convertResult(method.getReturnType());
	}

}
