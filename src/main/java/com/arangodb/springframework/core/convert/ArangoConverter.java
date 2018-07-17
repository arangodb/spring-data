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

import org.springframework.data.convert.EntityConverter;

import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 *
 */
@SuppressWarnings("deprecation")
public interface ArangoConverter
		extends EntityConverter<ArangoPersistentEntity<?>, ArangoPersistentProperty, Object, DBEntity>,
		ArangoEntityReader, ArangoEntityWriter {

	boolean isCollectionType(Class<?> type);

	boolean isEntityType(Class<?> type);

	ArangoTypeMapper getTypeMapper();

}
