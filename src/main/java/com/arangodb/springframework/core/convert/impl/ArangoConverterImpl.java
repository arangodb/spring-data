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

package com.arangodb.springframework.core.convert.impl;

import java.util.Optional;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import com.arangodb.velocypack.VPackBuilder;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.ValueType;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoConverterImpl implements ArangoConverter {

	private final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context;
	private final CustomConversions conversions;
	private final GenericConversionService conversionService;

	public ArangoConverterImpl(
		final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context,
		final CustomConversions conversions) {
		super();
		this.context = context;
		this.conversions = conversions;
		conversionService = new DefaultConversionService();
		conversions.registerConvertersIn(conversionService);
	}

	@Override
	public MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> getMappingContext() {
		return context;
	}

	@Override
	public <R> R read(final Class<R> type, final VPackSlice source) {
		return read(ClassTypeInformation.from(type), source);
	}

	protected <R> R read(final TypeInformation<R> type, final VPackSlice source) {
		return null;
	}

	@Override
	public void write(final Object source, final VPackBuilder sink) {
		if (source == null) {
			return;
		}
		write(source, ClassTypeInformation.from(source.getClass()), sink);
	}

	protected void write(final Object source, final ClassTypeInformation<?> type, final VPackBuilder sink) {
		final Optional<? extends ArangoPersistentEntity<?>> entity = context.getPersistentEntity(type);
		write(source, sink, entity, null);
	}

	protected void write(
		final Object source,
		final VPackBuilder sink,
		final Optional<? extends ArangoPersistentEntity<?>> entityC,
		final String fieldName) {
		final ArangoPersistentEntity<?> entity = entityC.orElseThrow(
			() -> new MappingException("No mapping metadata found for type " + source.getClass().getName()));

		final PersistentPropertyAccessor accessor = entity.getPropertyAccessor(source);

		sink.add(fieldName, ValueType.OBJECT);

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

		sink.close();
	}

	protected void writeProperty(
		final Object source,
		final VPackBuilder sink,
		final ArangoPersistentProperty property) {
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
		if (customWriteTarget.isPresent()) {
			// accessor.put(prop, conversionService.convert(obj, basicTargetType));
			final Object a = conversionService.convert(source, customWriteTarget.get());
			sink.add(fieldName, a.toString());// TODO
			return;
		}
		final TypeInformation<?> type = property.getTypeInformation();
		// write(source, sink, context.getPersistentEntity(type), fieldName);
		final Object a = conversionService.convert(source, type.getType());
		sink.add(fieldName, a.toString());// TODO
		return;
	}

}
