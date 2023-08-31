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

import java.util.Collection;
import java.util.Optional;

import com.arangodb.springframework.annotation.TtlIndex;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentEntity;

import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.springframework.annotation.FulltextIndex;
import com.arangodb.springframework.annotation.GeoIndex;
import com.arangodb.springframework.annotation.PersistentIndex;

/**
 * @author Mark Vollmary
 * @param <T>
 *
 */
public interface ArangoPersistentEntity<T>
		extends PersistentEntity<T, ArangoPersistentProperty>, ApplicationContextAware {

	String getCollection();

	CollectionCreateOptions getCollectionOptions();

	Optional<ArangoPersistentProperty> getArangoIdProperty();

	Optional<ArangoPersistentProperty> getRevProperty();

	Collection<PersistentIndex> getPersistentIndexes();

	Collection<GeoIndex> getGeoIndexes();

	Collection<FulltextIndex> getFulltextIndexes();

	Optional<TtlIndex> getTtlIndex();

	Collection<ArangoPersistentProperty> getPersistentIndexedProperties();

	Collection<ArangoPersistentProperty> getGeoIndexedProperties();

	Collection<ArangoPersistentProperty> getFulltextIndexedProperties();

	Optional<ArangoPersistentProperty> getTtlIndexedProperty();

	IdentifierAccessor getArangoIdAccessor(Object bean);

}
