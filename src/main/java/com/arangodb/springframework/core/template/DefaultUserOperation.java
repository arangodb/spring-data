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

package com.arangodb.springframework.core.template;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Permissions;
import com.arangodb.entity.UserEntity;
import com.arangodb.model.UserCreateOptions;
import com.arangodb.model.UserUpdateOptions;
import com.arangodb.springframework.core.UserOperations;

/**
 * @author Mark Vollmary
 *
 */
public class DefaultUserOperation implements UserOperations {

	private final ArangoDatabase db;
	private final String username;
	private final PersistenceExceptionTranslator exceptionTranslator;
	private final CollectionCallback collectionCallback;

	protected DefaultUserOperation(final ArangoDatabase db, final String username,
		final PersistenceExceptionTranslator exceptionTranslator, final CollectionCallback collectionCallback) {
		this.db = db;
		this.username = username;
		this.exceptionTranslator = exceptionTranslator;
		this.collectionCallback = collectionCallback;
	}

	@Override
	public UserEntity get() throws DataAccessException {
		try {
			return db.arango().getUser(username);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public UserEntity create(final String passwd, final UserCreateOptions options) throws DataAccessException {
		try {
			return db.arango().createUser(username, passwd);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public UserEntity update(final UserUpdateOptions options) throws DataAccessException {
		try {
			return db.arango().updateUser(username, options);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public UserEntity replace(final UserUpdateOptions options) throws DataAccessException {
		try {
			return db.arango().replaceUser(username, options);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public void delete() throws DataAccessException {
		try {
			db.arango().deleteUser(username);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public void grantDefaultDatabaseAccess(final Permissions permissions) throws DataAccessException {
		try {
			db.arango().grantDefaultDatabaseAccess(username, permissions);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public void grantDatabaseAccess(final Permissions permissions) throws DataAccessException {
		try {
			db.grantAccess(username, permissions);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public void resetDatabaseAccess() throws DataAccessException {
		try {
			db.resetAccess(username);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public void grantDefaultCollectionAccess(final Permissions permissions) throws DataAccessException {
		try {
			db.grantDefaultCollectionAccess(username, permissions);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public void grantCollectionAccess(final Class<?> type, final Permissions permissions) throws DataAccessException {
		collectionCallback.collection(type).grantAccess(username, permissions);
	}

	@Override
	public void grantCollectionAccess(final String name, final Permissions permissions) throws DataAccessException {
		collectionCallback.collection(name).grantAccess(username, permissions);
	}

	@Override
	public void resetCollectionAccess(final Class<?> type) throws DataAccessException {
		collectionCallback.collection(type).resetAccess(username);
	}

	@Override
	public void resetCollectionAccess(final String name) throws DataAccessException {
		collectionCallback.collection(name).resetAccess(username);
	}

	@Override
	public Permissions getDatabasePermissions() throws DataAccessException {
		try {
			return db.getPermissions(username);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public Permissions getCollectionPermissions(final Class<?> type) throws DataAccessException {
		return collectionCallback.collection(type).getPermissions(username);
	}

	@Override
	public Permissions getCollectionPermissions(final String name) throws DataAccessException {
		return collectionCallback.collection(name).getPermissions(name);
	}

	private RuntimeException translateException(RuntimeException e) {
		return DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
	}

}
