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

package com.arangodb.springframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Mark Vollmary
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Relations {

	public enum Direction {
		ANY, OUTBOUND, INBOUND
	}

	Class<?>[] edges();

	/**
	 * Edges and vertices returned by this query will start at the traversal depth
	 * of min (thus edges and vertices below will not be returned). If not
	 * specified, it defaults to 1. The minimal possible value is 0.
	 */
	int minDepth() default 1;

	/**
	 * Up to max length paths are traversed. If omitted, max defaults to min. Thus
	 * only the vertices and edges in the range of min are returned. max can not be
	 * specified without min.
	 */
	int maxDepth() default 1;

	/**
	 * Follow outgoing, incoming, or edges pointing in either direction in the
	 * traversal
	 */
	Direction direction() default Direction.ANY;

	/**
	 * Whether the entity should be loaded lazily
	 */
	boolean lazy() default false;

}
