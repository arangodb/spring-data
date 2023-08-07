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

package com.arangodb.springframework.core.convert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * This class contains additional converters that are not supported by the converter.
 * 
 * @author Christian Lechner
 *
 */
public class ArangoConverters {

	public static Collection<Converter<?, ?>> getConvertersToRegister() {
		final List<Converter<?, ?>> converters = new ArrayList<>();

		converters.add(StringToUuidConverter.INSTANCE);
		converters.add(UuidToStringConverter.INSTANCE);

		return converters;
	}

	@ReadingConverter
	public static enum StringToUuidConverter implements Converter<String, UUID> {
		INSTANCE;

		@Override
		public UUID convert(final String source) {
			return UUID.fromString(source);
		}
	}

	@WritingConverter
	public static enum UuidToStringConverter implements Converter<UUID, String> {
		INSTANCE;

		@Override
		public String convert(final UUID source) {
			return source.toString();
		}
	}

}
