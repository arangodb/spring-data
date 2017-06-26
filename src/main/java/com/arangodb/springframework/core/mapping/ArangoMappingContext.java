/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
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

import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

import com.arangodb.springframework.core.mapping.impl.ArangoPersistentEntityImpl;
import com.arangodb.springframework.core.mapping.impl.ArangoPersistentPropertyImpl;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoMappingContext
		extends AbstractMappingContext<ArangoPersistentEntityImpl<?>, ArangoPersistentProperty>
		implements ApplicationContextAware {

	private FieldNamingStrategy fieldNamingStrategy;
	private Optional<ApplicationContext> applicationContext;

	public ArangoMappingContext() {
		applicationContext = Optional.empty();
	}

	@Override
	protected <T> ArangoPersistentEntityImpl<?> createPersistentEntity(final TypeInformation<T> typeInformation) {
		final ArangoPersistentEntityImpl<T> entity = new ArangoPersistentEntityImpl<>(typeInformation);
		applicationContext.ifPresent(context -> entity.setApplicationContext(context));
		return entity;
	}

	@Override
	protected ArangoPersistentProperty createPersistentProperty(
		final Property property,
		final ArangoPersistentEntityImpl<?> owner,
		final SimpleTypeHolder simpleTypeHolder) {
		return new ArangoPersistentPropertyImpl(property, owner, simpleTypeHolder, fieldNamingStrategy);
	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = Optional.of(applicationContext);
	}

	public void setFieldNamingStrategy(final FieldNamingStrategy fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy;
	}

}
