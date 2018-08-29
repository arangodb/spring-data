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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.arangodb.entity.arangosearch.StoreValuesType;

/**
 * @author Mark Vollmary
 *
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ArangoSearchLinked {

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
