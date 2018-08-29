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

package com.arangodb.springframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.arangodb.entity.arangosearch.ConsolidationType;
import com.arangodb.entity.arangosearch.StoreValuesType;

/**
 * @author Mark Vollmary
 *
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ArangoSearch {

	/**
	 * @return The name of the arangosearch view
	 */
	String value() default "";

	/**
	 * @return Wait at least this many milliseconds between committing index data changes and making them visible to
	 *         queries (default: 60000, to disable use: 0). For the case where there are a lot of inserts/updates, a
	 *         lower value, until commit, will cause the index not to account for them and memory usage would continue
	 *         to grow. For the case where there are a few inserts/updates, a higher value will impact performance and
	 *         waste disk space for each commit call without any added benefits.
	 */
	long consolidationIntervalMsec() default -1;

	/**
	 * @return Wait at least this many commits between removing unused files in data directory (default: 10, to disable
	 *         use: 0). For the case where the consolidation policies merge segments often (i.e. a lot of
	 *         commit+consolidate), a lower value will cause a lot of disk space to be wasted. For the case where the
	 *         consolidation policies rarely merge segments (i.e. few inserts/deletes), a higher value will impact
	 *         performance without any added benefits.
	 */
	long cleanupIntervalStep() default -1;

	ConsolidationType consolidationType() default ConsolidationType.BYTES_ACCUM;

	/**
	 * @return Select a given segment for "consolidation" if and only if the formula based on type (as defined above)
	 *         evaluates to true, valid value range [0.0, 1.0] (default: 0.85)
	 */
	double consolidationThreshold() default -1;

	/**
	 * @return Apply the "consolidation" operation if and only if (default: 300): {segmentThreshold} <
	 *         number_of_segments
	 */
	long consolidationSegmentThreshold() default -1;

	/**
	 * @return The list of analyzers to be used for indexing of string values (default: ["identity"]).
	 */
	String[] analyzers() default {};

	/**
	 * @return The flag determines whether or not to index all fields on a particular level of depth (default: false).
	 */
	boolean includeAllFields() default false;

	/**
	 * @return The flag determines whether or not values in a lists should be treated separate (default: false).
	 */
	boolean trackListPositions() default false;

	/**
	 * @return How should the view track the attribute values, this setting allows for additional value retrieval
	 *         optimizations (default "none").
	 */
	StoreValuesType storeValues() default StoreValuesType.NONE;

}
