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

import java.util.Optional;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.util.StringUtils;

import com.arangodb.springframework.annotation.Field;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.FulltextIndexed;
import com.arangodb.springframework.annotation.GeoIndexed;
import com.arangodb.springframework.annotation.HashIndexed;
import com.arangodb.springframework.annotation.Key;
import com.arangodb.springframework.annotation.PersistentIndexed;
import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.annotation.Rev;
import com.arangodb.springframework.annotation.SkiplistIndexed;
import com.arangodb.springframework.annotation.To;

/**
 * @author Mark Vollmary
 *
 */
@SuppressWarnings("deprecation")
public class DefaultArangoPersistentProperty extends AnnotationBasedPersistentProperty<ArangoPersistentProperty>
		implements ArangoPersistentProperty {

	private final FieldNamingStrategy fieldNamingStrategy;

	public DefaultArangoPersistentProperty(final Property property,
		final PersistentEntity<?, ArangoPersistentProperty> owner, final SimpleTypeHolder simpleTypeHolder,
		final FieldNamingStrategy fieldNamingStrategy) {
		super(property, owner, simpleTypeHolder);
		this.fieldNamingStrategy = fieldNamingStrategy != null ? fieldNamingStrategy
				: PropertyNameFieldNamingStrategy.INSTANCE;
	}

	@Override
	protected Association<ArangoPersistentProperty> createAssociation() {
		return new Association<>(this, null);
	}

	@Override
	public boolean isKeyProperty() {
		return findAnnotation(Key.class) != null;
	}

	@Override
	public boolean isRevProperty() {
		return findAnnotation(Rev.class) != null;
	}

	@Override
	public Optional<Ref> getRef() {
		return Optional.ofNullable(findAnnotation(Ref.class));
	}

	@Override
	public Optional<Relations> getRelations() {
		return Optional.ofNullable(findAnnotation(Relations.class));
	}

	@Override
	public Optional<From> getFrom() {
		return Optional.ofNullable(findAnnotation(From.class));
	}

	@Override
	public Optional<To> getTo() {
		return Optional.ofNullable(findAnnotation(To.class));
	}

	@Override
	public String getFieldName() {
		final String fieldName;
		if (isIdProperty()) {
			fieldName = "_id";
		} else if (isKeyProperty()) {
			fieldName = "_key";
		} else if (isRevProperty()) {
			fieldName = "_rev";
		} else if (getFrom().isPresent()) {
			fieldName = "_from";
		} else if (getTo().isPresent()) {
			fieldName = "_to";
		} else {
			fieldName = getAnnotatedFieldName().orElse(fieldNamingStrategy.getFieldName(this));
		}
		return fieldName;
	}

	private Optional<String> getAnnotatedFieldName() {
		return Optional.ofNullable(findAnnotation(Field.class))
				.map(f -> StringUtils.hasText(f.value()) ? f.value() : null);
	}

	@Override
	public Optional<HashIndexed> getHashIndexed() {
		return Optional.ofNullable(findAnnotation(HashIndexed.class));
	}

	@Override
	public Optional<SkiplistIndexed> getSkiplistIndexed() {
		return Optional.ofNullable(findAnnotation(SkiplistIndexed.class));
	}

	@Override
	public Optional<PersistentIndexed> getPersistentIndexed() {
		return Optional.ofNullable(findAnnotation(PersistentIndexed.class));
	}

	@Override
	public Optional<GeoIndexed> getGeoIndexed() {
		return Optional.ofNullable(findAnnotation(GeoIndexed.class));
	}

	@Override
	public Optional<FulltextIndexed> getFulltextIndexed() {
		return Optional.ofNullable(findAnnotation(FulltextIndexed.class));
	}

}
