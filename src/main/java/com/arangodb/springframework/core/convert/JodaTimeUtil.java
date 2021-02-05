/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
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

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;

public final class JodaTimeUtil {

    private JodaTimeUtil() {
    }

    public static String format(final Instant source) {
        return ISODateTimeFormat.dateTime().print(source);
    }

    public static String format(final DateTime source) {
        return ISODateTimeFormat.dateTime().print(source);
    }

    public static String format(final LocalDate source) {
        return ISODateTimeFormat.yearMonthDay().print(source);
    }

    public static String format(final LocalDateTime source) {
        return ISODateTimeFormat.dateTime().print(source);
    }

    public static Instant parseInstant(final String source) {
        return Instant.parse(source);
    }

    public static DateTime parseDateTime(final String source) {
        return DateTime.parse(source);
    }

    public static LocalDate parseLocalDate(final String source) {
        return LocalDate.parse(source);
    }

    public static LocalDateTime parseLocalDateTime(final String source) {
        return LocalDateTime.parse(source);
    }

}