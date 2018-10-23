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

import org.springframework.dao.DataAccessException;

import com.arangodb.entity.Permissions;
import com.arangodb.entity.UserEntity;
import com.arangodb.model.UserCreateOptions;
import com.arangodb.model.UserUpdateOptions;

/**
 * Interface that specifies operations to manage ArangoDB users.
 *
 * @author Mark Vollmary
 *
 */
public interface UserOperations {

	/**
	 * Fetches data about the specified user. You can fetch information about yourself or you need permission to the
	 * _system database in order to execute this call.
	 *
	 * @return information about the user
	 * @throws DataAccessException
	 */
	UserEntity get() throws DataAccessException;

	/**
	 * Create a new user. This user will not have access to any database. You need permission to the _system database in
	 * order to execute this call.
	 *
	 * @param passwd
	 *            The user password
	 * @param options
	 *            Additional options, can be null
	 * @return information about the user
	 * @throws DataAccessException
	 */
	UserEntity create(String passwd, UserCreateOptions options) throws DataAccessException;

	/**
	 * Partially updates the data of an existing user. You can only change the password of your self. You need access to
	 * the _system database to change the active flag.
	 *
	 * @param options
	 *            Properties of the user to be changed
	 * @return information about the user
	 * @throws DataAccessException
	 */
	UserEntity update(UserUpdateOptions options) throws DataAccessException;

	/**
	 * Replaces the data of an existing user. You can only change the password of your self. You need access to the
	 * _system database to change the active flag.
	 *
	 * @param options
	 *            Additional properties of the user, can be null
	 * @return information about the user
	 * @throws DataAccessException
	 */
	UserEntity replace(UserUpdateOptions options) throws DataAccessException;

	/**
	 * Removes an existing user, identified by user. You need access to the _system database.
	 *
	 * @throws DataAccessException
	 */
	void delete() throws DataAccessException;

	/**
	 * Sets the default access level for databases for the user. You need permission to the _system database in order to
	 * execute this call.
	 *
	 * @param permissions
	 *            The permissions the user grant
	 * @since ArangoDB 3.2.0
	 * @throws DataAccessException
	 */
	void grantDefaultDatabaseAccess(Permissions permissions) throws DataAccessException;

	/**
	 * Grants or revoke access to the database for the user. You need permission to the _system database in order to
	 * execute this call.
	 *
	 * @param permissions
	 *            The permissions the user grant
	 * @throws DataAccessException
	 */
	void grantDatabaseAccess(Permissions permissions) throws DataAccessException;

	/**
	 * Clear the database access level, revert back to the default access level.
	 *
	 * @since ArangoDB 3.2.0
	 * @throws DataAccessException
	 */
	void resetDatabaseAccess() throws DataAccessException;

	/**
	 * Sets the default access level for collections for the user. You need permission to the _system database in order
	 * to execute this call.
	 *
	 * @param permissions
	 *            The permissions the user grant
	 * @since ArangoDB 3.2.0
	 * @throws DataAccessException
	 */
	void grantDefaultCollectionAccess(Permissions permissions) throws DataAccessException;

	/**
	 * Grants or revoke access to the collection for user. You need permission to the _system database in order to
	 * execute this call.
	 *
	 * @param entityClass
	 *            The entity type representing the collection
	 * @param permissions
	 *            The permissions the user grant
	 * @throws DataAccessException
	 */
	void grantCollectionAccess(Class<?> entityClass, Permissions permissions) throws DataAccessException;

	/**
	 * Grants or revoke access to the collection for user. You need permission to the _system database in order to
	 * execute this call.
	 *
	 * @param name
	 *            The name of the collection
	 * @param permissions
	 *            The permissions the user grant
	 * @throws DataAccessException
	 */
	void grantCollectionAccess(String name, Permissions permissions) throws DataAccessException;

	/**
	 * Clear the collection access level, revert back to the default access level.
	 *
	 * @param entityClass
	 *            The entity type representing the collection
	 * @throws DataAccessException
	 */
	void resetCollectionAccess(Class<?> entityClass) throws DataAccessException;

	/**
	 * Clear the collection access level, revert back to the default access level.
	 *
	 * @param name
	 *            The name of the collection
	 * @throws DataAccessException
	 */
	void resetCollectionAccess(String name) throws DataAccessException;

	/**
	 * Get specific database access level
	 *
	 * @return permissions of the user
	 * @since ArangoDB 3.2.0
	 * @throws DataAccessException
	 */
	Permissions getDatabasePermissions() throws DataAccessException;

	/**
	 * Get the collection access level
	 *
	 * @param entityClass
	 *            The entity type representing the collection
	 * @return permissions of the user
	 * @since ArangoDB 3.2.0
	 * @throws DataAccessException
	 */
	Permissions getCollectionPermissions(Class<?> entityClass) throws DataAccessException;

	/**
	 * Get the collection access level
	 *
	 * @param name
	 *            The name of the collection
	 * @return permissions of the user
	 * @since ArangoDB 3.2.0
	 * @throws DataAccessException
	 */
	Permissions getCollectionPermissions(String name) throws DataAccessException;

}
