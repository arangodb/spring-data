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

package com.arangodb.springframework.core.mapping;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoMappingContext
		extends AbstractMappingContext<DefaultArangoPersistentEntity<?>, ArangoPersistentProperty>
		implements ApplicationContextAware {

	private FieldNamingStrategy fieldNamingStrategy;
	private Optional<ApplicationContext> applicationContext;

	public ArangoMappingContext() {
		applicationContext = Optional.empty();
	}

	@Override
	protected <T> DefaultArangoPersistentEntity<?> createPersistentEntity(final TypeInformation<T> typeInformation) {
		final DefaultArangoPersistentEntity<T> entity = new DefaultArangoPersistentEntity<>(typeInformation);
		applicationContext.ifPresent(context -> entity.setApplicationContext(context));
		return entity;
	}

	@Override
	protected ArangoPersistentProperty createPersistentProperty(
		final Field field,
		final PropertyDescriptor descriptor,
		final DefaultArangoPersistentEntity<?> owner,
		final SimpleTypeHolder simpleTypeHolder) {
		return new DefaultArangoPersistentProperty(field, descriptor, owner, simpleTypeHolder, fieldNamingStrategy);
	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = Optional.of(applicationContext);
	}

	public void setFieldNamingStrategy(final FieldNamingStrategy fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy;
	}

}
