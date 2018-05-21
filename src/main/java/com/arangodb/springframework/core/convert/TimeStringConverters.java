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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import com.arangodb.velocypack.module.jdk8.internal.util.JavaTimeUtil;

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
		converters.add(OffsetDateTimeToStringConverter.INSTANCE);
		converters.add(ZonedDateTimeToStringConverter.INSTANCE);

		converters.add(StringToInstantConverter.INSTANCE);
		converters.add(StringToLocalDateConverter.INSTANCE);
		converters.add(StringToLocalDateTimeConverter.INSTANCE);
		converters.add(StringToOffsetDateTimeConverter.INSTANCE);
		converters.add(StringToZonedDateTimeConverter.INSTANCE);
		return converters;
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
