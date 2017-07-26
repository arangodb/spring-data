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

import org.springframework.data.mapping.PersistentProperty;

import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.FulltextIndexed;
import com.arangodb.springframework.annotation.GeoIndexed;
import com.arangodb.springframework.annotation.HashIndexed;
import com.arangodb.springframework.annotation.PersistentIndexed;
import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.annotation.SkiplistIndexed;
import com.arangodb.springframework.annotation.To;

/**
 * @author Mark Vollmary
 *
 */
public interface ArangoPersistentProperty extends PersistentProperty<ArangoPersistentProperty> {

	String getFieldName();

	boolean isKeyProperty();

	boolean isRevProperty();

	Optional<Ref> getRef();

	Optional<Relations> getRelations();

	Optional<From> getFrom();

	Optional<To> getTo();

	Optional<HashIndexed> getHashIndexed();

	Optional<SkiplistIndexed> getSkiplistIndexed();

	Optional<PersistentIndexed> getPersistentIndexed();

	Optional<GeoIndexed> getGeoIndexed();

	Optional<FulltextIndexed> getFulltextIndexed();

}
