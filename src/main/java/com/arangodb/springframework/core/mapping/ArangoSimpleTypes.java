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

package com.arangodb.springframework.core.mapping;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.mapping.model.SimpleTypeHolder;

import com.arangodb.springframework.core.convert.DBDocumentEntity;
import com.arangodb.velocypack.VPackSlice;

/**
 * This class contains additional types (besides the default ones) that are supported by the converter.
 * 
 * @author Christian Lechner
 *
 */
public final class ArangoSimpleTypes {

	private static final Set<Class<?>> ARANGO_SIMPLE_TYPES;

	static {
		final Set<Class<?>> simpleTypes = new HashSet<>();

		// com.arangodb.*
		simpleTypes.add(VPackSlice.class);
		simpleTypes.add(DBDocumentEntity.class);

		// java.math.*
		simpleTypes.add(BigInteger.class);
		simpleTypes.add(BigDecimal.class);

		// java.sql.*
		simpleTypes.add(Date.class);
		simpleTypes.add(Timestamp.class);

		// java.time.*
		simpleTypes.add(Instant.class);
		simpleTypes.add(LocalDate.class);
		simpleTypes.add(LocalDateTime.class);
		simpleTypes.add(OffsetDateTime.class);
		simpleTypes.add(ZonedDateTime.class);

		ARANGO_SIMPLE_TYPES = Collections.unmodifiableSet(simpleTypes);
	}

	public static final SimpleTypeHolder HOLDER = new SimpleTypeHolder(ARANGO_SIMPLE_TYPES, true);

	private ArangoSimpleTypes() {
	}

}