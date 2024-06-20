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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Slice;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoPage;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mapping.MappingException;
import org.springframework.util.Assert;

import com.arangodb.ArangoCursor;
import com.arangodb.springframework.core.ArangoOperations;

/**
 * Converts the result returned from the ArangoDB Java driver to the desired type.
 * 
 * @author Audrius Malele
 * @author Mark McCormick
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public class ArangoResultConverter<T> {

	private final static String MISSING_FULL_COUNT = "Query result does not contain the full result count! "
			+ "The most likely cause is a forgotten LIMIT clause in the query.";

	private final ArangoParameterAccessor accessor;
	private final ArangoCursor<?> result;
	private final ArangoOperations operations;
    private final Class<T> domainClass;


	/**
	 * @param accessor
     * @param result      the query result returned by the driver
     * @param operations  instance of arangoTemplate
     * @param domainClass class type of documents
	 */
	public ArangoResultConverter(final ArangoParameterAccessor accessor, final ArangoCursor<?> result,
                                 final ArangoOperations operations, final Class<T> domainClass) {
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
            return convert(type);
        } catch (final Exception e) {
            throw new MappingException(String.format("Can't convert result to type %s!", type.getName()), e);
        }
    }

    private Object convert(Class<?> type) {
			if (type.isArray()) {
            return convertArray();
        } else if (List.class.equals(type) || Iterable.class.equals(type) || Collection.class.equals(type)) {
            return convertList();
        } else if (Page.class.equals(type) || Slice.class.equals(type)) {
            return convertPage();
        } else if (Set.class.equals(type)) {
            return convertSet();
        } else if (ArangoCursor.class.equals(type)) {
            return convertArangoCursor();
        } else if (GeoResult.class.equals(type)) {
            return convertGeoResult();
        } else if (GeoResults.class.equals(type)) {
            return convertGeoResults();
        } else if (GeoPage.class.equals(type)) {
            return convertGeoPage();
        } else if (Optional.class.equals(type)) {
            return convertOptional();
        } else {
				return getNext(result);
			}
	}

	/**
	 * Creates a Set return type from the given cursor
	 * 
     * @param cursor query result from driver
	 * @return Set containing the results
	 */
	private Set<?> buildSet(final ArangoCursor<?> cursor) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(cursor, 0), false).collect(Collectors.toSet());
	}

	/**
	 * Build a GeoResult from the given ArangoCursor
	 *
     * @param cursor query result from driver
	 * @return GeoResult object
	 */
    private GeoResult<T> buildGeoResult(final ArangoCursor<JsonNode> cursor) {
        return buildGeoResult(cursor.next());
	}

	/**
	 * Construct a GeoResult from the given object
	 *
     * @param slice object representing one document in the result
	 * @return GeoResult object
	 */
    private GeoResult<T> buildGeoResult(final JsonNode slice) {
        JsonNode distSlice = slice.get("_distance");
        T entity = operations.getConverter().read(domainClass, slice);
		Distance distance = new Distance(distSlice.doubleValue() / 1000, Metrics.KILOMETERS).in(Metrics.NEUTRAL);
		return new GeoResult<>(entity, distance);
	}

	/**
	 * Build a GeoResults object with the ArangoCursor returned by query execution
	 *
     * @param cursor ArangoCursor containing query results
	 * @return GeoResults object with all results
	 */
    private GeoResults<T> buildGeoResults(final ArangoCursor<JsonNode> cursor) {
        final List<GeoResult<T>> list = new LinkedList<>();
        cursor.forEachRemaining(o -> list.add(buildGeoResult(o)));
        return new GeoResults<>(list);
	}

	public Optional<?> convertOptional() {
		return Optional.ofNullable(getNext(result));
	}

	public List<?> convertList() {
		return result.asListRemaining();
	}

	public PageImpl<?> convertPage() {
		Assert.notNull(result.getStats().getFullCount(), MISSING_FULL_COUNT);
		return new PageImpl<>(result.asListRemaining(), accessor.getPageable(), ((Number) result.getStats().getFullCount()).longValue());
	}

	public Set<?> convertSet() {
		return buildSet(result);
	}

	public ArangoCursor<?> convertArangoCursor() {
		return result;
	}

    @SuppressWarnings("unchecked")
    public GeoResult<T> convertGeoResult() {
        return buildGeoResult((ArangoCursor<JsonNode>) result);
	}

    @SuppressWarnings("unchecked")
    public GeoResults<T> convertGeoResults() {
        return buildGeoResults((ArangoCursor<JsonNode>) result);
	}

    @SuppressWarnings("unchecked")
    public GeoPage<T> convertGeoPage() {
		Assert.notNull(result.getStats().getFullCount(), MISSING_FULL_COUNT);
        return new GeoPage<>(buildGeoResults((ArangoCursor<JsonNode>) result), accessor.getPageable(), ((Number) result.getStats().getFullCount()).longValue());
	}

	public Object convertArray() {
		return result.asListRemaining().toArray();
	}

	private Object getNext(final ArangoCursor<?> cursor) {
		return cursor.hasNext() ? cursor.next() : null;
	}
}
