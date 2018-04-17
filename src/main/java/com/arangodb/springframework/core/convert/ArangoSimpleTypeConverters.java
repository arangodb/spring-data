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

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import com.arangodb.velocypack.internal.util.DateUtil;

/**
 * This class contains converters that are necessary to restore the original types defined in entities. Normally the
 * Java driver would do this, but he has no type information available (since he is deserializing to
 * {@link com.arangodb.springframework.core.convert.DBEntity} objects).
 * 
 * @author Christian Lechner
 *
 */
public class ArangoSimpleTypeConverters {

	public static Collection<Converter<?, ?>> getConvertersToRegister() {
		final List<Converter<?, ?>> converters = new ArrayList<>();

		converters.add(Base64StringToByteArrayConverter.INSTANCE);
		converters.add(StringToDateConverter.INSTANCE);
		converters.add(StringToSqlDateConverter.INSTANCE);
		converters.add(StringToSqlTimestampConverter.INSTANCE);

		return converters;
	}

	private static Date parse(final String source) {
		try {
			return DateUtil.parse(source);
		} catch (final ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@ReadingConverter
	public static enum Base64StringToByteArrayConverter implements Converter<String, byte[]> {
		INSTANCE;

		@Override
		public byte[] convert(final String source) {
			return DatatypeConverter.parseBase64Binary(source);
		}
	}

	@ReadingConverter
	public static enum StringToDateConverter implements Converter<String, Date> {
		INSTANCE;

		@Override
		public Date convert(final String source) {
			return parse(source);
		}
	}

	@ReadingConverter
	public static enum StringToSqlDateConverter implements Converter<String, java.sql.Date> {
		INSTANCE;

		@Override
		public java.sql.Date convert(final String source) {
			return new java.sql.Date(parse(source).getTime());
		}
	}

	@ReadingConverter
	public static enum StringToSqlTimestampConverter implements Converter<String, Timestamp> {
		INSTANCE;

		@Override
		public Timestamp convert(final String source) {
			return new Timestamp(parse(source).getTime());
		}
	}

}
