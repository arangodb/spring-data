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

package com.arangodb.springframework.repository.query;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Slice;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoPage;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.util.Assert;

import com.arangodb.ArangoCursor;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.convert.DBDocumentEntity;

/**
 * Converts the result returned from the ArangoDB Java driver to the desired type.
 * 
 * @author Audrius Malele
 * @author Mark McCormick
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public class ArangoResultConverter {

	private final static String MISSING_FULL_COUNT = "Query result does not contain the full result count! "
			+ "The most likely cause is a forgotten LIMIT clause in the query.";

	private final ArangoParameterAccessor accessor;
	private final ArangoCursor<?> result;
	private final ArangoOperations operations;
	private final Class<?> domainClass;

	private static Map<Object, Method> TYPE_MAP = new HashMap<>();

	/**
	 * Build static map of all supported return types and the method used to convert them
	 */
	static {
		try {
			TYPE_MAP.put(List.class, ArangoResultConverter.class.getMethod("convertList"));
			TYPE_MAP.put(Iterable.class, ArangoResultConverter.class.getMethod("convertList"));
			TYPE_MAP.put(Collection.class, ArangoResultConverter.class.getMethod("convertList"));
			TYPE_MAP.put(Page.class, ArangoResultConverter.class.getMethod("convertPage"));
			TYPE_MAP.put(Slice.class, ArangoResultConverter.class.getMethod("convertPage"));
			TYPE_MAP.put(Set.class, ArangoResultConverter.class.getMethod("convertSet"));
			TYPE_MAP.put(ArangoCursor.class, ArangoResultConverter.class.getMethod("convertArangoCursor"));
			TYPE_MAP.put(GeoResult.class, ArangoResultConverter.class.getMethod("convertGeoResult"));
			TYPE_MAP.put(GeoResults.class, ArangoResultConverter.class.getMethod("convertGeoResults"));
			TYPE_MAP.put(GeoPage.class, ArangoResultConverter.class.getMethod("convertGeoPage"));
			TYPE_MAP.put(Optional.class, ArangoResultConverter.class.getMethod("convertOptional"));
			TYPE_MAP.put("array", ArangoResultConverter.class.getMethod("convertArray"));
		} catch (final NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param accessor
	 * @param result
	 *            the query result returned by the driver
	 * @param operations
	 *            instance of arangoTemplate
	 * @param domainClass
	 *            class type of documents
	 */
	public ArangoResultConverter(final ArangoParameterAccessor accessor, final ArangoCursor<?> result,
		final ArangoOperations operations, final Class<?> domainClass) {
		this.accessor = accessor;
		this.result = result;
		this.operations = operations;
		this.domainClass = domainClass;
	}

	/**
	 * Called to convert result from ArangoCursor to given type, by invoking the appropriate converter method
	 * 
	 * @param type
	 * @return result in desired type
	 */
	public Object convertResult(final Class<?> type) {
		try {
			if (type.isArray()) {
				return TYPE_MAP.get("array").invoke(this);
			}
			if (!TYPE_MAP.containsKey(type)) {
				return getNext(result);
			}
			return TYPE_MAP.get(type).invoke(this);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Creates a Set return type from the given cursor
	 * 
	 * @param cursor
	 *            query result from driver
	 * @return Set containing the results
	 */
	private Set<?> buildSet(final ArangoCursor<?> cursor) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(cursor, 0), false).collect(Collectors.toSet());
	}

	/**
	 * Build a GeoResult from the given ArangoCursor
	 *
	 * @param cursor
	 *            query result from driver
	 * @return GeoResult object
	 */
	private GeoResult<?> buildGeoResult(final ArangoCursor<?> cursor) {
		GeoResult<?> geoResult = null;
		while (cursor.hasNext() && geoResult == null) {
			final Object object = cursor.next();
			@SuppressWarnings("unchecked")
			final Map<String, Object> map = (Map<String, Object>) object;
			final Double distanceInMeters = (Double) map.get("_distance");
			if (distanceInMeters == null) {
				continue;
			}
			final Object entity = operations.getConverter().read(domainClass, new DBDocumentEntity(map));
			final Distance distance = new Distance(distanceInMeters / 1000, Metrics.KILOMETERS);
			geoResult = new GeoResult<>(entity, distance);
		}
		return geoResult;
	}

	/**
	 * Construct a GeoResult from the given object
	 *
	 * @param object
	 *            object representing one document in the result
	 * @return GeoResult object
	 */
	private GeoResult<?> buildGeoResult(final Object object) {
		if (object == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		final Map<String, Object> map = (Map<String, Object>) object;
		final Object entity = operations.getConverter().read(domainClass, new DBDocumentEntity(map));
		final Double distanceInMeters = (Double) map.get("_distance");
		if (distanceInMeters == null) {
			return null;
		}
		final Distance distance = new Distance(distanceInMeters / 1000, Metrics.KILOMETERS);
		return new GeoResult<>(entity, distance);
	}

	/**
	 * Build a GeoResults object with the ArangoCursor returned by query execution
	 *
	 * @param cursor
	 *            ArangoCursor containing query results
	 * @return GeoResults object with all results
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private GeoResults<?> buildGeoResults(final ArangoCursor<?> cursor) {
		final List<GeoResult<?>> list = new LinkedList<>();
		cursor.forEachRemaining(o -> {
			final GeoResult<?> geoResult = buildGeoResult(o);
			if (geoResult != null) {
				list.add(geoResult);
			}
		});
		return new GeoResults(list);
	}

	public Optional<?> convertOptional() {
		return Optional.ofNullable(getNext(result));
	}

	public List<?> convertList() {
		return result.asListRemaining();
	}

	public PageImpl<?> convertPage() {
		Assert.notNull(result.getStats().getFullCount(), MISSING_FULL_COUNT);
		return new PageImpl<>(result.asListRemaining(), accessor.getPageable(), result.getStats().getFullCount());
	}

	public Set<?> convertSet() {
		return buildSet(result);
	}

	public ArangoCursor<?> convertArangoCursor() {
		return result;
	}

	public GeoResult<?> convertGeoResult() {
		return buildGeoResult(result);
	}

	public GeoResults<?> convertGeoResults() {
		return buildGeoResults(result);
	}

	public GeoPage<?> convertGeoPage() {
		return new GeoPage<>(buildGeoResults(result), accessor.getPageable(), result.getStats().getFullCount());
	}

	public Object convertArray() {
		return result.asListRemaining().toArray();
	}

	private Object getNext(final ArangoCursor<?> cursor) {
		return cursor.hasNext() ? cursor.next() : null;
	}
}
