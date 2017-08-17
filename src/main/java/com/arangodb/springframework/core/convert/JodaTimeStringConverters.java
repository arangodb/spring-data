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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.ClassUtils;

import com.arangodb.ArangoDBException;
import com.arangodb.velocypack.internal.util.DateUtil;

/**
 * @author Mark Vollmary
 *
 */
public class JodaTimeStringConverters {

	private static final boolean JODA_TIME_IS_PRESENT = ClassUtils.isPresent("org.joda.time.LocalDate", null);

	public static Collection<Converter<?, ?>> getConvertersToRegister() {
		if (!JODA_TIME_IS_PRESENT) {
			return Collections.emptySet();
		}
		final List<Converter<?, ?>> converters = new ArrayList<>();
		converters.add(InstantToStringConverter.INSTANCE);
		converters.add(DateTimeToStringConverter.INSTANCE);
		converters.add(LocalDateToStringConverter.INSTANCE);
		converters.add(LocalDateTimeToStringConverter.INSTANCE);

		converters.add(StringToInstantConverter.INSTANCE);
		converters.add(StringToDateTimeConverter.INSTANCE);
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
			return source == null ? null : DateUtil.format(source.toDate());
		}
	}

	public static enum DateTimeToStringConverter implements Converter<DateTime, String> {
		INSTANCE;

		@Override
		public String convert(final DateTime source) {
			return source == null ? null : DateUtil.format(source.toDate());
		}
	}

	public static enum LocalDateToStringConverter implements Converter<LocalDate, String> {
		INSTANCE;

		@Override
		public String convert(final LocalDate source) {
			return source == null ? null : DateUtil.format(source.toDate());
		}
	}

	public static enum LocalDateTimeToStringConverter implements Converter<LocalDateTime, String> {
		INSTANCE;

		@Override
		public String convert(final LocalDateTime source) {
			return source == null ? null : DateUtil.format(source.toDate());
		}
	}

	public static enum StringToInstantConverter implements Converter<String, Instant> {
		INSTANCE;

		@Override
		public Instant convert(final String source) {
			return source == null ? null : new Instant(parse(source).getTime());
		}
	}

	public static enum StringToDateTimeConverter implements Converter<String, DateTime> {
		INSTANCE;

		@Override
		public DateTime convert(final String source) {
			return source == null ? null : new DateTime(parse(source).getTime());
		}
	}

	public static enum StringToLocalDateConverter implements Converter<String, LocalDate> {
		INSTANCE;

		@Override
		public LocalDate convert(final String source) {
			return source == null ? null : new LocalDate(parse(source).getTime());
		}
	}

	public static enum StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
		INSTANCE;

		@Override
		public LocalDateTime convert(final String source) {
			return source == null ? null : new LocalDateTime(parse(source).getTime());
		}
	}

}
