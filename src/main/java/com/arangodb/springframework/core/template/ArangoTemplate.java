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

package com.arangodb.springframework.core.template;

import java.util.Collection;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import com.arangodb.ArangoDB;
import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.LogEntity;
import com.arangodb.entity.LogLevelEntity;
import com.arangodb.entity.ServerRole;
import com.arangodb.entity.UserEntity;
import com.arangodb.model.LogOptions;
import com.arangodb.model.UserCreateOptions;
import com.arangodb.model.UserUpdateOptions;
import com.arangodb.springframework.core.ArangoDatabaseOperations;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.util.ArangoExceptionTranslator;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.Response;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoTemplate extends ArangoTemplateBase implements ArangoOperations {

	private final ArangoDB arango;

	public ArangoTemplate(final ArangoDB.Builder arango) {
		this(arango, new ArangoExceptionTranslator());
	}

	public ArangoTemplate(final ArangoDB.Builder arango, final PersistenceExceptionTranslator exceptionTranslator) {
		super(exceptionTranslator);
		this.arango = arango.build();
	}

	@Override
	public ArangoDatabaseOperations db() {
		return new ArangoDatabaseTemplate(arango.db(), exceptionTranslator);
	}

	@Override
	public ArangoDatabaseOperations db(final String name) {
		return new ArangoDatabaseTemplate(arango.db(name), exceptionTranslator);
	}

	@Override
	public Boolean createDatabase(final String name) throws DataAccessException {
		try {
			return arango.createDatabase(name);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Collection<String> getDatabases() throws DataAccessException {
		try {
			return arango.getDatabases();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Collection<String> getAccessibleDatabases() throws DataAccessException {
		try {
			return arango.getAccessibleDatabases();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Collection<String> getAccessibleDatabasesFor(final String user) throws DataAccessException {
		try {
			return arango.getAccessibleDatabasesFor(user);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public ArangoDBVersion getVersion() throws DataAccessException {
		try {
			return arango.getVersion();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public ServerRole getRole() throws DataAccessException {
		try {
			return arango.getRole();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public UserEntity createUser(final String user, final String passwd) throws DataAccessException {
		try {
			return arango.createUser(user, passwd);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public UserEntity createUser(final String user, final String passwd, final UserCreateOptions options)
			throws DataAccessException {
		try {
			return arango.createUser(user, passwd, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public void deleteUser(final String user) throws DataAccessException {
		try {
			arango.deleteUser(user);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public UserEntity getUser(final String user) throws DataAccessException {
		try {
			return arango.getUser(user);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Collection<UserEntity> getUsers() throws DataAccessException {
		try {
			return arango.getUsers();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public UserEntity updateUser(final String user, final UserUpdateOptions options) throws DataAccessException {
		try {
			return arango.updateUser(user, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public UserEntity replaceUser(final String user, final UserUpdateOptions options) throws DataAccessException {
		try {
			return arango.replaceUser(user, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Response execute(final Request request) throws DataAccessException {
		try {
			return arango.execute(request);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public LogEntity getLogs(final LogOptions options) throws DataAccessException {
		try {
			return arango.getLogs(options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public LogLevelEntity getLogLevel() throws DataAccessException {
		try {
			return arango.getLogLevel();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public LogLevelEntity setLogLevel(final LogLevelEntity entity) throws DataAccessException {
		try {
			return arango.setLogLevel(entity);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

}
