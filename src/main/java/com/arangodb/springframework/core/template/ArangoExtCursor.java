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

package com.arangodb.springframework.core.template;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.CursorEntity;
import com.arangodb.internal.ArangoCursorExecute;
import com.arangodb.internal.ArangoCursorImpl;
import com.arangodb.internal.ArangoCursorIterator;
import com.arangodb.internal.InternalArangoDatabase;
import com.arangodb.springframework.core.convert.ArangoConverter;

/**
 * @author Mark Vollmary
 * @param <T>
 *
 */
class ArangoExtCursor<T> extends ArangoCursorImpl<T> {

	protected ArangoExtCursor(final InternalArangoDatabase<?, ?> db, final ArangoCursorExecute execute,
		final Class<T> type, final CursorEntity result, final ArangoConverter converter) {
		super(db, execute, type, result);
		ArangoExtCursorIterator.class.cast(iterator).setConverter(converter);
	}

	@Override
	protected ArangoCursorIterator<T> createIterator(
		final ArangoCursor<T> cursor,
		final InternalArangoDatabase<?, ?> db,
		final ArangoCursorExecute execute,
		final CursorEntity result) {
		return new ArangoExtCursorIterator<>(cursor, db, execute, result);
	}
}
