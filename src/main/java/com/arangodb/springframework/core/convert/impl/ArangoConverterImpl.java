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

package com.arangodb.springframework.core.convert.impl;

import java.util.Optional;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.DBEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoConverterImpl implements ArangoConverter {

	private final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context;
	private final CustomConversions conversions;
	private final GenericConversionService conversionService;
	private final EntityInstantiators instantiators;

	public ArangoConverterImpl(
		final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context,
		final CustomConversions conversions) {
		super();
		this.context = context;
		this.conversions = conversions;
		conversionService = new DefaultConversionService();
		conversions.registerConvertersIn(conversionService);
		instantiators = new EntityInstantiators();
	}

	@Override
	public MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> getMappingContext() {
		return context;
	}

	@Override
	public <R> R read(final Class<R> type, final DBEntity source) {
		return read(ClassTypeInformation.from(type), source);
	}

	@SuppressWarnings("unchecked")
	protected <R> R read(final TypeInformation<R> type, final DBEntity source) {
		if (source == null) {
			return null;
		}
		if (conversions.hasCustomReadTarget(type.getType(), type.getType())) {
			return conversionService.convert(source, type.getType());
		}
		// TODO isCollection or Array or Map
		final Optional<? extends ArangoPersistentEntity<R>> entity = (Optional<? extends ArangoPersistentEntity<R>>) context
				.getPersistentEntity(type.getType());
		return read(type, source, entity);
	}

	protected <R> R read(
		final TypeInformation<R> type,
		final DBEntity source,
		final Optional<? extends ArangoPersistentEntity<R>> entityC) {
		final ArangoPersistentEntity<R> entity = entityC.orElseThrow(
			() -> new MappingException("No mapping metadata found fot type " + type.getType().getName()));

		final EntityInstantiator instantiatorFor = instantiators.getInstantiatorFor(entity);
		final ParameterValueProvider<ArangoPersistentProperty> provider = null;
		final R instance = instantiatorFor.createInstance(entity, provider);
		final ConvertingPropertyAccessor accessor = new ConvertingPropertyAccessor(entity.getPropertyAccessor(instance),
				conversionService);

		entity.doWithProperties((final ArangoPersistentProperty property) -> {
			readProperty(accessor, source.get(property.getFieldName()), property);
		});
		return instance;
	}

	protected void readProperty(
		final ConvertingPropertyAccessor accessor,
		final Object source,
		final ArangoPersistentProperty property) {
		accessor.setProperty(property, Optional.ofNullable(source));
	}

	@Override
	public void write(final Object source, final DBEntity sink) {
		if (source == null) {
			return;
		}
		write(source, ClassTypeInformation.from(source.getClass()), sink);
	}

	protected void write(final Object source, final ClassTypeInformation<?> type, final DBEntity sink) {
		final Optional<? extends ArangoPersistentEntity<?>> entity = context.getPersistentEntity(type);
		write(source, sink, entity);
	}

	protected void write(
		final Object source,
		final DBEntity sink,
		final Optional<? extends ArangoPersistentEntity<?>> entityC) {
		final ArangoPersistentEntity<?> entity = entityC.orElseThrow(
			() -> new MappingException("No mapping metadata found for type " + source.getClass().getName()));

		final PersistentPropertyAccessor accessor = entity.getPropertyAccessor(source);

		entity.doWithProperties((final ArangoPersistentProperty property) -> {
			if (!property.isWritable()) {
				return;
			}
			final Optional<Object> propertyObj = accessor.getProperty(property);
			propertyObj.ifPresent(proObj -> {
				writeProperty(proObj, sink, property);
			});
		});
		entity.doWithAssociations((final Association<ArangoPersistentProperty> association) -> {
			final ArangoPersistentProperty inverse = association.getInverse();
			final Optional<Object> property = accessor.getProperty(inverse);
			property.ifPresent(prop -> {
				writeProperty(prop, sink, inverse);
			});
		});
	}

	protected void writeProperty(final Object source, final DBEntity sink, final ArangoPersistentProperty property) {
		if (source == null) {
			return;
		}
		final String fieldName = property.getFieldName();
		final TypeInformation<?> valueType = ClassTypeInformation.from(source.getClass());
		if (valueType.isCollectionLike()) {
			// TODO
			return;
		}
		if (valueType.isMap()) {
			// TODO
			return;
		}
		if (property.getRef().isPresent()) {
			// TODO
			return;
		}
		// TODO from, to

		final Optional<Class<?>> customWriteTarget = conversions.getCustomWriteTarget(source.getClass());
		final Class<?> targetType = customWriteTarget.orElseGet(() -> property.getTypeInformation().getType());
		sink.put(fieldName, conversionService.convert(source, targetType));
		return;
	}

}
