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
import com.arangodb.springframework.core.convert.ArangoConverter;

/**
 * 
 * @author Mark Vollmary
 * @author Christian Lechner
 */
class ArangoCursorInitializer implements com.arangodb.util.ArangoCursorInitializer {

	private final ArangoConverter converter;
	private final ApplicationEventPublisher eventPublisher;

	public ArangoCursorInitializer(final ArangoConverter converter) {
		this(converter, null);
	}

	public ArangoCursorInitializer(final ArangoConverter converter, final ApplicationEventPublisher eventPublisher) {
		this.converter = converter;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public <T> ArangoCursor<T> createInstance(
			final InternalArangoDatabase<?, ?> db,
			final ArangoCursorExecute execute,
			final Class<T> type,
			final InternalCursorEntity result) {
		return new ArangoExtCursor<>(db, execute, type, result, converter, eventPublisher);
	}

}
