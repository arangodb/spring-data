/*
 * Copyright 2015-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arangodb.springframework.core.geo;

import org.springframework.data.geo.Point;

import java.util.List;

/**
 * {@link GeoJsonLineString} is defined as list of at least 2 {@link Point}s.
 *
 * @author Christoph Strobl
 * @see <a href="https://geojson.org/geojson-spec.html#linestring">https://geojson.org/geojson-spec.html#linestring</a>
 */
public class GeoJsonLineString extends GeoJsonMultiPoint {

	private static final String TYPE = "LineString";

	/**
	 * Creates a new {@link GeoJsonLineString} for the given {@link Point}s.
	 *
	 * @param points must not be {@literal null} and have at least 2 entries.
	 */
	public GeoJsonLineString(List<Point> points) {
		super(points);
	}

	/**
	 * Creates a new {@link GeoJsonLineString} for the given {@link Point}s.
	 *
	 * @param first must not be {@literal null}
	 * @param second must not be {@literal null}
	 * @param others can be {@literal null}
	 */
	public GeoJsonLineString(Point first, Point second, Point... others) {
		super(first, second, others);
	}

	@Override
	public String getType() {
		return TYPE;
	}
}
