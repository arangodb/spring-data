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

import org.springframework.context.ApplicationListener;
import org.springframework.core.GenericTypeResolver;

/**
 * Base class to implement domain class specific event-handler methods.
 * 
 * @author Christian Lechner
 */
public abstract class AbstractArangoEventListener<T> implements ApplicationListener<ArangoMappingEvent<?>> {

	private final Class<?> domainClass;

	public AbstractArangoEventListener() {
		final Class<?> typeArgument = GenericTypeResolver.resolveTypeArgument(this.getClass(),
			AbstractArangoEventListener.class);
		this.domainClass = typeArgument != null ? typeArgument : Object.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final void onApplicationEvent(final ArangoMappingEvent<?> event) {
		final Object source = event.getSource();

		if (source == null) {
			return;
		}

		if (event instanceof AbstractDeleteEvent) {
			final Class<?> eventDomainClass = ((AbstractDeleteEvent<?>) event).getType();

			if (eventDomainClass != null && domainClass.isAssignableFrom(eventDomainClass)) {
				if (event instanceof BeforeDeleteEvent) {
					onBeforeDelete((BeforeDeleteEvent<T>) event);
				}

				else if (event instanceof AfterDeleteEvent) {
					onAfterDelete((AfterDeleteEvent<T>) event);
				}
			}
			return;
		}

		if (!domainClass.isAssignableFrom(source.getClass())) {
			return;
		}

		if (event instanceof AfterLoadEvent) {
			onAfterLoad((AfterLoadEvent<T>) event);
		} else if (event instanceof BeforeSaveEvent) {
			onBeforeSave((BeforeSaveEvent<T>) event);
		} else if (event instanceof AfterSaveEvent) {
			onAfterSave((AfterSaveEvent<T>) event);
		}
	}

	/**
	 * Captures {@link AfterLoadEvent}s. Default implementation is a no-op.
	 * 
	 * @param event
	 *            never null
	 */
	public void onAfterLoad(final AfterLoadEvent<T> event) {
		// do nothing
	}

	/**
	 * Captures {@link BeforeSaveEvent}s. Default implementation is a no-op.
	 * 
	 * @param event
	 *            never null
	 */
	public void onBeforeSave(final BeforeSaveEvent<T> event) {
		// do nothing
	}

	/**
	 * Captures {@link AfterSaveEvent}s. Default implementation is a no-op.
	 * 
	 * @param event
	 *            never null
	 */
	public void onAfterSave(final AfterSaveEvent<T> event) {
		// do nothing
	}

	/**
	 * Captures {@link BeforeDeleteEvent}s. Default implementation is a no-op.
	 * 
	 * @param event
	 *            never null
	 */
	public void onBeforeDelete(final BeforeDeleteEvent<T> event) {
		// do nothing
	}

	/**
	 * Captures {@link AfterDeleteEvent}s. Default implementation is a no-op.
	 * 
	 * @param event
	 *            never null
	 */
	public void onAfterDelete(final AfterDeleteEvent<T> event) {
		// do nothing
	}

}
