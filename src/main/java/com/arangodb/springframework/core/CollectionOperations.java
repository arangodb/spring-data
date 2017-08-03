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

package com.arangodb.springframework.core;

import java.util.Collection;

import org.springframework.dao.DataAccessException;

import com.arangodb.entity.IndexEntity;
import com.arangodb.entity.Permissions;
import com.arangodb.model.FulltextIndexOptions;
import com.arangodb.model.GeoIndexOptions;
import com.arangodb.model.HashIndexOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.arangodb.model.SkiplistIndexOptions;

/**
 * @author Mark Vollmary
 *
 */
public interface CollectionOperations {

	void drop() throws DataAccessException;

	void truncate() throws DataAccessException;

	long count() throws DataAccessException;

	Collection<IndexEntity> getIndexes() throws DataAccessException;

	IndexEntity ensureHashIndex(Collection<String> fields, HashIndexOptions options) throws DataAccessException;

	IndexEntity ensureSkiplistIndex(Collection<String> fields, SkiplistIndexOptions options) throws DataAccessException;

	IndexEntity ensurePersistentIndex(Collection<String> fields, PersistentIndexOptions options)
			throws DataAccessException;

	IndexEntity ensureGeoIndex(Collection<String> fields, GeoIndexOptions options) throws DataAccessException;

	IndexEntity ensureFulltextIndex(Collection<String> fields, FulltextIndexOptions options) throws DataAccessException;

	void dropIndex(String id) throws DataAccessException;

	void grantAccess(String username, Permissions permissions) throws DataAccessException;

	void resetAccess(String username) throws DataAccessException;

	Permissions getPermissions(String username) throws DataAccessException;

}
