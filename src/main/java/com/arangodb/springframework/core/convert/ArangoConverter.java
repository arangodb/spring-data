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

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;

import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 *
 */
public interface ArangoConverter extends ArangoEntityReader, ArangoEntityWriter {

	void readProperty(
			ArangoPersistentEntity<?> entity,
			PersistentPropertyAccessor<?> accessor,
			JsonNode source,
			ArangoPersistentProperty property
	);

	boolean isCollectionType(Class<?> type);

	boolean isEntityType(Class<?> type);

	ArangoTypeMapper getTypeMapper();

	MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> getMappingContext();

	ConversionService getConversionService();

	<T> T convertIfNecessary(Object source, Class<T> type);

	String convertId(Object id);

}
