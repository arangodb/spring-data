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

package com.arangodb.springframework.core.mapping.event;

/**
 * Base class for delete events.
 * 
 * @author Christian Lechner
 */
public abstract class AbstractDeleteEvent<T> extends ArangoMappingEvent<Object> {

	private static final long serialVersionUID = 1L;

	private final Class<T> type;

	public AbstractDeleteEvent(final Object id, final Class<T> type) {
		super(id);
		this.type = type;
	}

	/**
	 * Returns the {@code _id} or {@code _key} of the (to be) deleted entity.
	 */
	@Override
	public Object getSource() {
		return super.getSource();
	}

	/**
	 * Returns the class of the (to be) deleted entity.
	 */
	public Class<T> getType() {
		return type;
	}

}
