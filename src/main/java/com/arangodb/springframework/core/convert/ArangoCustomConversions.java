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

package com.arangodb.springframework.core.convert;

import java.util.Collection;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoCustomConversions extends CustomConversions {

	// private static final StoreConversions STORE_CONVERSIONS;

	// static {
	// final Collection<?> converters = new ArrayList<>();
	// STORE_CONVERSIONS = StoreConversions.of(SimpleTypeHolder.DEFAULT, converters);
	// }

	public ArangoCustomConversions(final Collection<?> converters) {
		super(converters);
	}

}
