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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;

import com.arangodb.springframework.core.mapping.ArangoSimpleTypes;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 *
 */
public class ArangoCustomConversions extends CustomConversions {

	private static final StoreConversions STORE_CONVERSIONS;

	static {
		final Collection<Converter<?, ?>> storeConverters = new ArrayList<>();
		storeConverters.addAll(TimeStringConverters.getConvertersToRegister());
		storeConverters.addAll(JodaTimeStringConverters.getConvertersToRegister());
		storeConverters.addAll(ArangoSimpleTypeConverters.getConvertersToRegister());

		STORE_CONVERSIONS = StoreConversions.of(ArangoSimpleTypes.HOLDER,
			Collections.unmodifiableCollection(storeConverters));
	}

	public ArangoCustomConversions(final Collection<?> converters) {
		super(STORE_CONVERSIONS, converters);
	}

}
