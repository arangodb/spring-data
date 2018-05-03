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

package com.arangodb.springframework.repository;

import org.springframework.data.repository.core.support.PersistentEntityInformation;

import java.io.Serializable;

import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;

/**
 *
 * @author Christian Lechner
 */
public class ArangoPersistentEntityInformation<T, ID extends Serializable> extends PersistentEntityInformation<T, ID>
		implements ArangoEntityInformation<T, ID> {

	private final ArangoPersistentEntity<T> persistentEntity;

	public ArangoPersistentEntityInformation(final ArangoPersistentEntity<T> entity) {
		super(entity);
		this.persistentEntity = entity;
	}

	@Override
	public String getCollection() {
		return persistentEntity.getCollection();
	}

}
