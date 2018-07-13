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

import org.springframework.data.repository.query.Param;

import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.testdata.IdTestEntity;

/**
 * @author Mark Vollmary
 * 
 */
public interface IdTestRepository<ID> extends ArangoRepository<IdTestEntity<ID>, ID> {

	@Query("FOR i IN idTestEntity FILTER i._key == @id RETURN i")
	Optional<IdTestEntity<ID>> findByQuery(@Param("id") ID id);

	@Query("FOR i IN idTestEntity FILTER i._key == @id RETURN i._key")
	Optional<ID> findIdByQuery(@Param("id") ID id);

	@Query("FOR i IN idTestEntity FILTER i._key == @entity._key RETURN i")
	Optional<IdTestEntity<ID>> findByEntity(@Param("entity") IdTestEntity<ID> entity);
}
