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

import org.springframework.context.ApplicationEventPublisher;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.CursorEntity;
import com.arangodb.internal.ArangoCursorExecute;
import com.arangodb.internal.InternalArangoDatabase;
import com.arangodb.internal.cursor.ArangoCursorIterator;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.mapping.event.AfterLoadEvent;
import com.arangodb.springframework.core.mapping.event.ArangoMappingEvent;
import com.arangodb.velocypack.VPackSlice;

/**
 * 
 * @author Mark Vollmary
 * @author Christian Lechner
 */
class ArangoExtCursorIterator<T> extends ArangoCursorIterator<T> {

	private ArangoConverter converter;
	private ApplicationEventPublisher eventPublisher;

	protected ArangoExtCursorIterator(final ArangoCursor<T> cursor, final InternalArangoDatabase<?, ?> db,
		final ArangoCursorExecute execute, final CursorEntity result) {
		super(cursor, execute, db, result);
	}

	public void setConverter(final ArangoConverter converter) {
		this.converter = converter;
	}

	public void setEventPublisher(final ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	protected <R> R deserialize(final VPackSlice source, final Class<R> type) {
		final R result = converter.read(type, source);
		if (result != null) {
			potentiallyEmitEvent(new AfterLoadEvent<>(result));
		}
		return result;
	}

	private void potentiallyEmitEvent(final ArangoMappingEvent<?> event) {
		if (eventPublisher != null) {
			eventPublisher.publishEvent(event);
		}
	}

}
