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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.mapping.model.SimpleTypeHolder;

import com.arangodb.springframework.core.convert.DBEntity;

/**
 * @author Christian Lechner
 *
 */
public abstract class ArangoSimpleTypes {

	private static final Set<Class<?>> ARANGO_SIMPLE_TYPES;

	static {
		Set<Class<?>> simpleTypes = new HashSet<Class<?>>();
		simpleTypes.add(DBEntity.class);

		ARANGO_SIMPLE_TYPES = Collections.unmodifiableSet(simpleTypes);
	}

	public static final SimpleTypeHolder HOLDER = new SimpleTypeHolder(ARANGO_SIMPLE_TYPES, true);

	private ArangoSimpleTypes() {
	}

}