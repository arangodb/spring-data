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
 * @author Mark Vollmary
 *
 */
public interface UserOperations {

	UserEntity get() throws DataAccessException;

	UserEntity create(String passwd, UserCreateOptions options) throws DataAccessException;

	UserEntity update(UserUpdateOptions options) throws DataAccessException;

	UserEntity replace(UserUpdateOptions options) throws DataAccessException;

	void delete() throws DataAccessException;

	void grantDefaultDatabaseAccess(Permissions permissions) throws DataAccessException;

	void grantDatabaseAccess(Permissions permissions) throws DataAccessException;

	void resetDatabaseAccess() throws DataAccessException;

	void grantDefaultCollectionAccess(Permissions permissions) throws DataAccessException;

	void grantCollectionAccess(Class<?> type, Permissions permissions) throws DataAccessException;

	void grantCollectionAccess(String name, Permissions permissions) throws DataAccessException;

	void resetCollectionAccess(Class<?> type) throws DataAccessException;

	void resetCollectionAccess(String name) throws DataAccessException;

	Permissions getDatabasePermissions() throws DataAccessException;

	Permissions getCollectionPermissions(Class<?> type) throws DataAccessException;

	Permissions getCollectionPermissions(String name) throws DataAccessException;

}
