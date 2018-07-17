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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.mapping.model.SimpleTypeHolder;

import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.springframework.core.convert.DBEntity;
import com.arangodb.velocypack.VPackSlice;

/**
 * This class contains all types that are directly supported by the Java driver (through java-velocypack).
 * 
 * @author Christian Lechner
 *
 */
@SuppressWarnings("deprecation")
public abstract class ArangoSimpleTypes {

	private static final Set<Class<?>> ARANGO_SIMPLE_TYPES;

	static {
		final Set<Class<?>> simpleTypes = new HashSet<>();

		// com.arangodb.*
		simpleTypes.add(DBEntity.class);
		simpleTypes.add(BaseDocument.class);
		simpleTypes.add(BaseEdgeDocument.class);
		simpleTypes.add(VPackSlice.class);

		// primitives
		simpleTypes.add(boolean.class);
		simpleTypes.add(byte.class);
		simpleTypes.add(char.class);
		simpleTypes.add(short.class);
		simpleTypes.add(int.class);
		simpleTypes.add(long.class);
		simpleTypes.add(float.class);
		simpleTypes.add(double.class);

		// primitive arrays
		simpleTypes.add(boolean[].class);
		simpleTypes.add(byte[].class);
		simpleTypes.add(char[].class);
		simpleTypes.add(short[].class);
		simpleTypes.add(int[].class);
		simpleTypes.add(long[].class);
		simpleTypes.add(float[].class);
		simpleTypes.add(double[].class);

		// java.lang.*
		simpleTypes.add(Boolean.class);
		simpleTypes.add(Byte.class);
		simpleTypes.add(Character.class);
		simpleTypes.add(Short.class);
		simpleTypes.add(Integer.class);
		simpleTypes.add(Long.class);
		simpleTypes.add(Float.class);
		simpleTypes.add(Double.class);
		simpleTypes.add(Number.class);
		simpleTypes.add(String.class);
		simpleTypes.add(Enum.class);

		// java.math.*
		simpleTypes.add(BigInteger.class);
		simpleTypes.add(BigDecimal.class);

		// java.util.*
		simpleTypes.add(UUID.class);
		simpleTypes.add(Date.class);

		// java.sql.*
		simpleTypes.add(java.sql.Date.class);
		simpleTypes.add(Timestamp.class);

		// java.time.*
		simpleTypes.add(Instant.class);
		simpleTypes.add(LocalDate.class);
		simpleTypes.add(LocalDateTime.class);
		simpleTypes.add(OffsetDateTime.class);
		simpleTypes.add(ZonedDateTime.class);

		ARANGO_SIMPLE_TYPES = Collections.unmodifiableSet(simpleTypes);
	}

	public static final SimpleTypeHolder HOLDER = new SimpleTypeHolder(ARANGO_SIMPLE_TYPES, false);

	private ArangoSimpleTypes() {
	}

}