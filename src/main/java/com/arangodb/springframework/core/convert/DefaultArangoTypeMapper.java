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

import java.util.Arrays;
import java.util.List;

import org.springframework.data.convert.DefaultTypeMapper;
import org.springframework.data.convert.SimpleTypeInformationMapper;
import org.springframework.data.convert.TypeAliasAccessor;
import org.springframework.data.convert.TypeInformationMapper;
import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;

/**
 * @author Christian Lechner
 *
 */
@SuppressWarnings("deprecation")
public class DefaultArangoTypeMapper extends DefaultTypeMapper<DBEntity> implements ArangoTypeMapper {

	public static final String DEFAULT_TYPE_KEY = "_class";

	private final String typeKey;

	public DefaultArangoTypeMapper() {
		this(DEFAULT_TYPE_KEY);
	}

	public DefaultArangoTypeMapper(final String typeKey) {
		this(typeKey, Arrays.asList(new SimpleTypeInformationMapper()));
	}

	public DefaultArangoTypeMapper(final String typeKey,
		final MappingContext<? extends PersistentEntity<?, ?>, ?> mappingContext) {
		this(typeKey, new DocumentTypeAliasAccessor(typeKey), mappingContext,
				Arrays.asList(new SimpleTypeInformationMapper()));
	}

	public DefaultArangoTypeMapper(final String typeKey, final List<? extends TypeInformationMapper> mappers) {
		this(typeKey, new DocumentTypeAliasAccessor(typeKey), null, mappers);
	}

	private DefaultArangoTypeMapper(final String typeKey, final TypeAliasAccessor<DBEntity> accessor,
		final MappingContext<? extends PersistentEntity<?, ?>, ?> mappingContext,
		final List<? extends TypeInformationMapper> mappers) {

		super(accessor, mappingContext, mappers);
		this.typeKey = typeKey;
	}

	@Override
	public boolean isTypeKey(final String key) {
		return typeKey == null ? false : typeKey.equals(key);
	}

	public static final class DocumentTypeAliasAccessor implements TypeAliasAccessor<DBEntity> {

		private final String typeKey;

		public DocumentTypeAliasAccessor(final String typeKey) {
			this.typeKey = typeKey;
		}

		@Override
		public Alias readAliasFrom(final DBEntity source) {
			if (source instanceof DBCollectionEntity) {
				return Alias.NONE;
			}

			if (source instanceof DBDocumentEntity) {
				return Alias.ofNullable(source.get(this.typeKey));
			}

			throw new IllegalArgumentException("Cannot read alias from " + source.getClass());
		}

		@Override
		public void writeTypeTo(final DBEntity sink, final Object alias) {
			if (this.typeKey != null && sink instanceof DBDocumentEntity) {
				sink.put(this.typeKey, alias);
			}
		}

	}

}
