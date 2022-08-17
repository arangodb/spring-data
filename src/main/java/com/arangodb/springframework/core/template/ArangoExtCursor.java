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

import com.arangodb.internal.cursor.entity.InternalCursorEntity;
import org.springframework.context.ApplicationEventPublisher;

import com.arangodb.ArangoCursor;
import com.arangodb.internal.ArangoCursorExecute;
import com.arangodb.internal.InternalArangoDatabase;
import com.arangodb.internal.cursor.ArangoCursorImpl;
import com.arangodb.internal.cursor.ArangoCursorIterator;
import com.arangodb.springframework.core.convert.ArangoConverter;

/**
 * 
 * @author Mark Vollmary
 * @author Christian Lechner
 */
class ArangoExtCursor<T> extends ArangoCursorImpl<T> {

	protected ArangoExtCursor(final InternalArangoDatabase<?, ?> db, final ArangoCursorExecute execute,
							  final Class<T> type, final InternalCursorEntity result, final ArangoConverter converter,
							  final ApplicationEventPublisher eventPublisher) {
		super(db, execute, type, result);
		final ArangoExtCursorIterator<?> it = (ArangoExtCursorIterator<?>) iterator;
		it.setConverter(converter);
		it.setEventPublisher(eventPublisher);
	}

	@Override
	protected ArangoCursorIterator<T> createIterator(
		final ArangoCursor<T> cursor,
		final InternalArangoDatabase<?, ?> db,
		final ArangoCursorExecute execute,
		final InternalCursorEntity result) {
		return new ArangoExtCursorIterator<>(cursor, db, execute, result);
	}
}
