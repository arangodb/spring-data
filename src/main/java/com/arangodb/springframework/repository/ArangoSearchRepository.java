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

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

/**
 * @author Mark Vollmary
 *
 */
@NoRepositoryBean
public interface ArangoSearchRepository<T> extends QueryByExampleExecutor<T>, Repository<T, String> {

	/**
	 * Retrieves an entity by its id.
	 * 
	 * @param id
	 *            must not be {@literal null}.
	 * @return the entity with the given id or {@literal Optional#empty()} if none found
	 * @throws IllegalArgumentException
	 *             if {@code id} is {@literal null}.
	 */
	Optional<T> findById(String id);

	/**
	 * Returns whether an entity with the given id exists.
	 * 
	 * @param id
	 *            must not be {@literal null}.
	 * @return {@literal true} if an entity with the given id exists, {@literal false} otherwise.
	 * @throws IllegalArgumentException
	 *             if {@code id} is {@literal null}.
	 */
	boolean existsById(String id);

	/**
	 * Returns all instances of the type.
	 * 
	 * @return all entities
	 */
	Iterable<T> findAll();

	/**
	 * Returns all instances of the type with the given IDs.
	 * 
	 * @param ids
	 * @return
	 */
	Iterable<T> findAllById(Iterable<String> ids);

	/**
	 * Returns the number of entities available.
	 * 
	 * @return the number of entities
	 */
	long count();

	/**
	 * Returns all entities sorted by the given options.
	 * 
	 * @param sort
	 * @return all entities sorted by the given options
	 */
	Iterable<T> findAll(Sort sort);

	/**
	 * Returns a {@link Page} of entities meeting the paging restriction provided in the {@code Pageable} object.
	 * 
	 * @param pageable
	 * @return a page of entities
	 */
	Page<T> findAll(Pageable pageable);

}
