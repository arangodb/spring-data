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

import com.arangodb.entity.CollectionPropertiesEntity;
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

	/**
	 * Return the collection name
	 * 
	 * @return collection name
	 */
	String name();

	/**
	 * Deletes the collection from the database.
	 * 
	 * @throws DataAccessException
	 */
	void drop() throws DataAccessException;

	/**
	 * Removes all documents from the collection, but leaves the indexes intact
	 * 
	 * @throws DataAccessException
	 */
	void truncate() throws DataAccessException;

	/**
	 * Counts the documents in a collection
	 * 
	 * @return number of
	 * @throws DataAccessException
	 */
	long count() throws DataAccessException;

	/**
	 * Reads the properties of the specified collection
	 * 
	 * @return properties of the collection
	 * @throws DataAccessException
	 */
	CollectionPropertiesEntity getProperties() throws DataAccessException;

	/**
	 * Returns all indexes of the collection
	 * 
	 * @return information about the indexes
	 * @throws DataAccessException
	 */
	Collection<IndexEntity> getIndexes() throws DataAccessException;

	/**
	 * Creates a hash index for the collection if it does not already exist.
	 * 
	 * @param fields
	 *            A list of attribute paths
	 * @param options
	 *            Additional options, can be null
	 * @return information about the index
	 * @throws DataAccessException
	 */
	IndexEntity ensureHashIndex(Iterable<String> fields, HashIndexOptions options) throws DataAccessException;

	/**
	 * Creates a skip-list index for the collection, if it does not already exist.
	 * 
	 * @param fields
	 *            A list of attribute paths
	 * @param options
	 *            Additional options, can be null
	 * @return information about the index
	 * @throws DataAccessException
	 */
	IndexEntity ensureSkiplistIndex(Iterable<String> fields, SkiplistIndexOptions options) throws DataAccessException;

	/**
	 * Creates a persistent index for the collection, if it does not already exist.
	 * 
	 * @param fields
	 *            A list of attribute paths
	 * @param options
	 *            Additional options, can be null
	 * @return information about the index
	 * @throws DataAccessException
	 */
	IndexEntity ensurePersistentIndex(Iterable<String> fields, PersistentIndexOptions options)
			throws DataAccessException;

	/**
	 * Creates a geo-spatial index for the collection, if it does not already exist.
	 * 
	 * @param fields
	 *            A list of attribute paths
	 * @param options
	 *            Additional options, can be null
	 * @return information about the index
	 * @throws DataAccessException
	 */
	IndexEntity ensureGeoIndex(Iterable<String> fields, GeoIndexOptions options) throws DataAccessException;

	/**
	 * Creates a fulltext index for the collection, if it does not already exist.
	 * 
	 * @param fields
	 *            A list of attribute paths
	 * @param options
	 *            Additional options, can be null
	 * @return information about the index
	 * @throws DataAccessException
	 */
	IndexEntity ensureFulltextIndex(Iterable<String> fields, FulltextIndexOptions options) throws DataAccessException;

	/**
	 * Deletes the index with the given {@code id} from the collection.
	 * 
	 * @param id
	 *            The index-handle
	 * @throws DataAccessException
	 */
	void dropIndex(String id) throws DataAccessException;

	/**
	 * Grants or revoke access to the collection for user user. You need permission to the _system database in order to
	 * execute this call.
	 * 
	 * @param username
	 *            The name of the user
	 * @param permissions
	 *            The permissions the user grant
	 * @throws DataAccessException
	 */
	void grantAccess(String username, Permissions permissions) throws DataAccessException;

	/**
	 * Clear the collection access level, revert back to the default access level.
	 * 
	 * @param username
	 *            The name of the user
	 * @since ArangoDB 3.2.0
	 * @throws DataAccessException
	 */
	void resetAccess(String username) throws DataAccessException;

	/**
	 * Get the collection access level
	 * 
	 * @param username
	 *            The name of the user
	 * @return permissions of the user
	 * @since ArangoDB 3.2.0
	 * @throws DataAccessException
	 */
	Permissions getPermissions(String username) throws DataAccessException;

}
