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

package com.arangodb.springframework.core.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.convert.MappingContextTypeInformationMapper;
import org.springframework.data.convert.SimpleTypeInformationMapper;
import org.springframework.data.convert.TypeInformationMapper;
import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import com.arangodb.velocypack.VPackBuilder;
import com.arangodb.velocypack.VPackSlice;

/**
 * @author Christian Lechner
 *
 */
public class DefaultArangoTypeMapper implements ArangoTypeMapper {

	public static final String DEFAULT_TYPE_KEY = "_class";

	private final String typeKey;
	private final ArangoTypeAliasAccessor accessor;
	private final List<? extends TypeInformationMapper> mappers;
	private final Map<Alias, Optional<TypeInformation<?>>> typeCache;

	public DefaultArangoTypeMapper() {
		this(DEFAULT_TYPE_KEY);
	}

	public DefaultArangoTypeMapper(final String typeKey) {
		this(typeKey, Arrays.asList(new SimpleTypeInformationMapper()));
	}

	public DefaultArangoTypeMapper(final String typeKey,
		final MappingContext<? extends PersistentEntity<?, ?>, ?> mappingContext) {
		this(typeKey, new DefaultTypeAliasAccessor(typeKey), mappingContext,
				Arrays.asList(new SimpleTypeInformationMapper()));
	}

	public DefaultArangoTypeMapper(final String typeKey, final List<? extends TypeInformationMapper> mappers) {
		this(typeKey, new DefaultTypeAliasAccessor(typeKey), null, mappers);
	}

	private DefaultArangoTypeMapper(final String typeKey, final ArangoTypeAliasAccessor accessor,
		final MappingContext<? extends PersistentEntity<?, ?>, ?> mappingContext,
		final List<? extends TypeInformationMapper> additionalMappers) {

		Assert.notNull(accessor, "Accessor must not be null!");
		Assert.notNull(additionalMappers, "AdditionalMappers must not be null!");

		final List<TypeInformationMapper> mappers = new ArrayList<>(additionalMappers.size() + 1);
		if (mappingContext != null) {
			mappers.add(new MappingContextTypeInformationMapper(mappingContext));
		}
		mappers.addAll(additionalMappers);

		this.mappers = Collections.unmodifiableList(mappers);
		this.accessor = accessor;
		this.typeCache = new ConcurrentHashMap<>(16, 0.75f, 4);
		this.typeKey = typeKey;
	}

	@Override
	public TypeInformation<?> readType(final VPackSlice source) {
		Assert.notNull(source, "Source must not be null!");
		return getFromCacheOrCreate(accessor.readAliasFrom(source));
	}

	@Override
	public <T> TypeInformation<? extends T> readType(final VPackSlice source, final TypeInformation<T> basicType) {
		Assert.notNull(source, "Source must not be null!");
		Assert.notNull(basicType, "Basic type must not be null!");

		final TypeInformation<?> documentsTargetType = readType(source);

		if (documentsTargetType == null) {
			return basicType;
		}

		final Class<T> rawType = basicType.getType();

		final boolean isMoreConcreteCustomType = rawType == null
				|| rawType.isAssignableFrom(documentsTargetType.getType()) && !rawType.equals(documentsTargetType);

		if (!isMoreConcreteCustomType) {
			return basicType;
		}

		final ClassTypeInformation<?> targetType = ClassTypeInformation.from(documentsTargetType.getType());

		return (TypeInformation<? extends T>) basicType.specialize(targetType);
	}
	
	public void writeType(final Class<?> type, final VPackBuilder sink) {
		writeType(ClassTypeInformation.from(type), sink);
	}
	
	public void writeType(final TypeInformation<?> info, final VPackBuilder sink) {
		Assert.notNull(info, "TypeInformation must not be null!");

		final Alias alias = getAliasFor(info);
		if (alias.isPresent()) {
			accessor.writeTypeTo(sink, alias.getValue());
		}
	}

	@Override
	public boolean isTypeKey(final String key) {
		return typeKey == null ? false : typeKey.equals(key);
	}
	
	protected final Alias getAliasFor(final TypeInformation<?> info) {
		Assert.notNull(info, "TypeInformation must not be null!");
		
		for (final TypeInformationMapper mapper : mappers) {
			final Alias alias = mapper.createAliasFor(info);
			if (alias.isPresent()) {
				return alias;
			}
		}

		return Alias.NONE;
	}

	private TypeInformation<?> getFromCacheOrCreate(final Alias alias) {
		return typeCache.computeIfAbsent(alias, key -> {
			for (final TypeInformationMapper mapper : mappers) {
				final TypeInformation<?> typeInformation = mapper.resolveTypeFrom(key);

				if (typeInformation != null) {
					return Optional.of(typeInformation);
				}
			}
			return Optional.empty();
		}).orElse(null);
	}

	public static final class DefaultTypeAliasAccessor implements ArangoTypeAliasAccessor {

		private final String typeKey;

		public DefaultTypeAliasAccessor(final String typeKey) {
			this.typeKey = typeKey;
		}

		public Alias readAliasFrom(final VPackSlice source) {
			if (source.isArray()) {
				return Alias.NONE;
			}

			if (source.isObject()) {
				final VPackSlice typeKey = source.get(this.typeKey);
				return Alias.ofNullable(typeKey.isString() ? typeKey.getAsString() : null);
			}

			throw new IllegalArgumentException("Cannot read alias from VPack type " + source.getType());
		}

		public void writeTypeTo(final VPackBuilder sink, final Object alias) {
			if (this.typeKey != null) {
				sink.add(this.typeKey, alias.toString());
			}
		}

	}

}
