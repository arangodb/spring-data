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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.JodaTimeConverters;
import org.springframework.data.convert.WritingConverter;

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
		storeConverters.addAll(JodaTimeConverters.getConvertersToRegister());
		storeConverters.addAll(TimeStringConverters.getConvertersToRegister());
		storeConverters.addAll(JodaTimeStringConverters.getConvertersToRegister());
		
		STORE_CONVERSIONS = StoreConversions.of(ArangoSimpleTypes.HOLDER,
			Collections.unmodifiableCollection(storeConverters));
	}

	public ArangoCustomConversions(final Collection<?> converters) {
		super(STORE_CONVERSIONS, converters);
	}

	@WritingConverter
	private enum CustomToStringConverter implements GenericConverter {

		INSTANCE;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.core.convert.converter.GenericConverter#getConvertibleTypes()
		 */
		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {

			final ConvertiblePair localeToString = new ConvertiblePair(Locale.class, String.class);
			final ConvertiblePair booleanToString = new ConvertiblePair(Character.class, String.class);

			return new HashSet<>(Arrays.asList(localeToString, booleanToString));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.core.convert.converter.GenericConverter#convert(java.lang.Object,
		 * org.springframework.core.convert.TypeDescriptor, org.springframework.core.convert.TypeDescriptor)
		 */
		@Override
		public Object convert(final Object source, final TypeDescriptor sourceType, final TypeDescriptor targetType) {
			return source.toString();
		}
	}
}
