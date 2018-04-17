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

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.core.convert.converter.Converter;

import com.arangodb.ArangoDBException;
import com.arangodb.velocypack.internal.util.DateUtil;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 *
 */
public class TimeStringConverters {

	public static Collection<Converter<?, ?>> getConvertersToRegister() {
		final List<Converter<?, ?>> converters = new ArrayList<>();
		converters.add(InstantToStringConverter.INSTANCE);
		converters.add(LocalDateToStringConverter.INSTANCE);
		converters.add(LocalDateTimeToStringConverter.INSTANCE);

		converters.add(StringToInstantConverter.INSTANCE);
		converters.add(StringToLocalDateConverter.INSTANCE);
		converters.add(StringToLocalDateTimeConverter.INSTANCE);
		return converters;
	}

	private static Date parse(final String source) {
		try {
			return DateUtil.parse(source);
		} catch (final ParseException e) {
			throw new ArangoDBException(e);
		}
	}

	public static enum InstantToStringConverter implements Converter<Instant, String> {
		INSTANCE;

		@Override
		public String convert(final Instant source) {
			return source == null ? null : DateUtil.format(Date.from(source));
		}
	}

	public static enum LocalDateToStringConverter implements Converter<LocalDate, String> {
		INSTANCE;

		@Override
		public String convert(final LocalDate source) {
			return source == null ? null
					: DateUtil.format(Date.from(source.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		}
	}

	public static enum LocalDateTimeToStringConverter implements Converter<LocalDateTime, String> {
		INSTANCE;

		@Override
		public String convert(final LocalDateTime source) {
			return source == null ? null
					: DateUtil.format(Date.from(source.atZone(ZoneId.systemDefault()).toInstant()));
		}
	}

	public static enum StringToInstantConverter implements Converter<String, Instant> {
		INSTANCE;

		@Override
		public Instant convert(final String source) {
			return source == null ? null : parse(source).toInstant();
		}
	}

	public static enum StringToLocalDateConverter implements Converter<String, LocalDate> {
		INSTANCE;

		@Override
		public LocalDate convert(final String source) {
			return source == null ? null : parse(source).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}
	}

	public static enum StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
		INSTANCE;

		@Override
		public LocalDateTime convert(final String source) {
			return source == null ? null : parse(source).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		}
	}

}
