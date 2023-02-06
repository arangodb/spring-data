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
import java.time.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;


/**
 * These date and java.time converters are necessary to override (possibly existing) Spring converters.
 * 
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public class TimeStringConverters {

	public static Collection<Converter<?, ?>> getConvertersToRegister() {
		final List<Converter<?, ?>> converters = new ArrayList<>();
		converters.add(DateToStringConverter.INSTANCE);
		converters.add(InstantToStringConverter.INSTANCE);
		converters.add(LocalDateToStringConverter.INSTANCE);
		converters.add(LocalDateTimeToStringConverter.INSTANCE);
		converters.add(LocalTimeToStringConverter.INSTANCE);
		converters.add(OffsetDateTimeToStringConverter.INSTANCE);
		converters.add(ZonedDateTimeToStringConverter.INSTANCE);

		converters.add(StringToDateConverter.INSTANCE);
		converters.add(StringToInstantConverter.INSTANCE);
		converters.add(StringToLocalDateConverter.INSTANCE);
		converters.add(StringToLocalDateTimeConverter.INSTANCE);
		converters.add(StringToLocalTimeConverter.INSTANCE);
		converters.add(StringToOffsetDateTimeConverter.INSTANCE);
		converters.add(StringToZonedDateTimeConverter.INSTANCE);
		return converters;
	}

	private static Date parseDate(final String source) {
		try {
			return source == null ? null : JavaTimeUtil.parse(source);
		} catch (final ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@WritingConverter
	public static enum DateToStringConverter implements Converter<Date, String> {
		INSTANCE;

		@Override
		public String convert(final Date source) {
			return source == null ? null : JavaTimeUtil.format(source);
		}
	}

	@ReadingConverter
	public static enum StringToDateConverter implements Converter<String, Date> {
		INSTANCE;

		@Override
		public Date convert(final String source) {
			return parseDate(source);
		}
	}

	@WritingConverter
	public static enum InstantToStringConverter implements Converter<Instant, String> {
		INSTANCE;

		@Override
		public String convert(final Instant source) {
			return source == null ? null : JavaTimeUtil.format(source);
		}
	}

	@WritingConverter
	public static enum LocalDateToStringConverter implements Converter<LocalDate, String> {
		INSTANCE;

		@Override
		public String convert(final LocalDate source) {
			return source == null ? null : JavaTimeUtil.format(source);
		}
	}

	@WritingConverter
	public static enum LocalDateTimeToStringConverter implements Converter<LocalDateTime, String> {
		INSTANCE;

		@Override
		public String convert(final LocalDateTime source) {
			return source == null ? null : JavaTimeUtil.format(source);
		}
	}

	@WritingConverter
	public static enum LocalTimeToStringConverter implements Converter<LocalTime, String> {
		INSTANCE;

		@Override
		public String convert(final LocalTime source) {
			return source == null ? null : JavaTimeUtil.format(source);
		}
	}

	@WritingConverter
	public static enum OffsetDateTimeToStringConverter implements Converter<OffsetDateTime, String> {
		INSTANCE;

		@Override
		public String convert(final OffsetDateTime source) {
			return source == null ? null : JavaTimeUtil.format(source);
		}
	}

	@WritingConverter
	public static enum ZonedDateTimeToStringConverter implements Converter<ZonedDateTime, String> {
		INSTANCE;

		@Override
		public String convert(final ZonedDateTime source) {
			return source == null ? null : JavaTimeUtil.format(source);
		}
	}

	@ReadingConverter
	public static enum StringToInstantConverter implements Converter<String, Instant> {
		INSTANCE;

		@Override
		public Instant convert(final String source) {
			return source == null ? null : JavaTimeUtil.parseInstant(source);
		}
	}

	@ReadingConverter
	public static enum StringToLocalDateConverter implements Converter<String, LocalDate> {
		INSTANCE;

		@Override
		public LocalDate convert(final String source) {
			return source == null ? null : JavaTimeUtil.parseLocalDate(source);
		}
	}

	@ReadingConverter
	public static enum StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
		INSTANCE;

		@Override
		public LocalDateTime convert(final String source) {
			return source == null ? null : JavaTimeUtil.parseLocalDateTime(source);
		}
	}

	@ReadingConverter
	public static enum StringToLocalTimeConverter implements Converter<String, LocalTime> {
		INSTANCE;

		@Override
		public LocalTime convert(final String source) {
			return source == null ? null : JavaTimeUtil.parseLocalTime(source);
		}
	}

	@ReadingConverter
	public static enum StringToOffsetDateTimeConverter implements Converter<String, OffsetDateTime> {
		INSTANCE;

		@Override
		public OffsetDateTime convert(final String source) {
			return source == null ? null : JavaTimeUtil.parseOffsetDateTime(source);
		}
	}

	@ReadingConverter
	public static enum StringToZonedDateTimeConverter implements Converter<String, ZonedDateTime> {
		INSTANCE;

		@Override
		public ZonedDateTime convert(final String source) {
			return source == null ? null : JavaTimeUtil.parseZonedDateTime(source);
		}
	}

}
