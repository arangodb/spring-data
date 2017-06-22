/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
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

import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.LogEntity;
import com.arangodb.entity.LogLevelEntity;
import com.arangodb.entity.ServerRole;
import com.arangodb.entity.UserEntity;
import com.arangodb.model.LogOptions;
import com.arangodb.model.UserCreateOptions;
import com.arangodb.model.UserUpdateOptions;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.Response;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public interface ArangoOperations {

	ArangoDatabaseOperations db();

	ArangoDatabaseOperations db(final String name);

	LogLevelEntity setLogLevel(final LogLevelEntity entity) throws DataAccessException;

	LogLevelEntity getLogLevel() throws DataAccessException;

	LogEntity getLogs(final LogOptions options) throws DataAccessException;

	Response execute(final Request request) throws DataAccessException;

	UserEntity replaceUser(final String user, final UserUpdateOptions options) throws DataAccessException;

	UserEntity updateUser(final String user, final UserUpdateOptions options) throws DataAccessException;

	Collection<UserEntity> getUsers() throws DataAccessException;

	UserEntity getUser(final String user) throws DataAccessException;

	void deleteUser(final String user) throws DataAccessException;

	UserEntity createUser(final String user, final String passwd, final UserCreateOptions options)
			throws DataAccessException;

	UserEntity createUser(final String user, final String passwd) throws DataAccessException;

	ServerRole getRole() throws DataAccessException;

	ArangoDBVersion getVersion() throws DataAccessException;

	Collection<String> getAccessibleDatabasesFor(final String user) throws DataAccessException;

	Collection<String> getAccessibleDatabases() throws DataAccessException;

	Collection<String> getDatabases() throws DataAccessException;

	Boolean createDatabase(final String name) throws DataAccessException;

}
