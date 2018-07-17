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

import com.arangodb.velocypack.VPackDeserializationContext;
import com.arangodb.velocypack.VPackDeserializer;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.exception.VPackException;

/**
 * @author Mark Vollmary
 *
 */
@SuppressWarnings("deprecation")
public class DBEntityDeserializer implements VPackDeserializer<DBEntity> {

	@Override
	public DBEntity deserialize(
		final VPackSlice parent,
		final VPackSlice vpack,
		final VPackDeserializationContext context) throws VPackException {
		final Class<?> type = vpack.isObject() ? DBDocumentEntity.class : DBCollectionEntity.class;
		return (DBEntity) context.deserialize(vpack, type);
	}

}
