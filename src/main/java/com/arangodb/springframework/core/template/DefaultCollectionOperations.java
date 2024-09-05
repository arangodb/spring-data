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

import java.util.Collection;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.CollectionPropertiesEntity;
import com.arangodb.entity.IndexEntity;
import com.arangodb.entity.Permissions;
import com.arangodb.model.FulltextIndexOptions;
import com.arangodb.model.GeoIndexOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.arangodb.model.TtlIndexOptions;
import com.arangodb.springframework.core.CollectionOperations;

/**
 * @author Mark Vollmary
 *
 */
public class DefaultCollectionOperations implements CollectionOperations {

	private final ArangoCollection collection;
	private final Map<CollectionCacheKey, CollectionCacheValue> collectionCache;
	private final PersistenceExceptionTranslator exceptionTranslator;

	protected DefaultCollectionOperations(final ArangoCollection collection,
		final Map<CollectionCacheKey, CollectionCacheValue> collectionCache,
		final PersistenceExceptionTranslator exceptionTranslator) {
		this.collection = collection;
		this.collectionCache = collectionCache;
		this.exceptionTranslator = exceptionTranslator;
	}

	@Override
	public String name() {
		return collection.name();
	}

	@Override
	public void drop() throws DataAccessException {
		collectionCache.remove(new CollectionCacheKey(collection.db().name(), collection.name()));
		try {
			collection.drop();
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public void truncate() throws DataAccessException {
		try {
			collection.truncate();
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public long count() throws DataAccessException {
		try {
			final Long count = collection.count().getCount();
			return count != null ? count : -1L;
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public CollectionPropertiesEntity getProperties() throws DataAccessException {
		try {
			return collection.getProperties();
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public Collection<IndexEntity> getIndexes() throws DataAccessException {
		try {
			return collection.getIndexes();
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public IndexEntity ensurePersistentIndex(final Iterable<String> fields, final PersistentIndexOptions options)
			throws DataAccessException {
		try {
			return collection.ensurePersistentIndex(fields, options);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public IndexEntity ensureGeoIndex(final Iterable<String> fields, final GeoIndexOptions options)
			throws DataAccessException {
		try {
			return collection.ensureGeoIndex(fields, options);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	@Deprecated
	public IndexEntity ensureFulltextIndex(final Iterable<String> fields, final FulltextIndexOptions options)
			throws DataAccessException {
		try {
			return collection.ensureFulltextIndex(fields, options);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public IndexEntity ensureTtlIndex(Iterable<String> fields, TtlIndexOptions options) throws DataAccessException {
		try {
			return collection.ensureTtlIndex(fields, options);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public void dropIndex(final String id) throws DataAccessException {
		try {
			collection.deleteIndex(id);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public void grantAccess(final String username, final Permissions permissions) {
		try {
			collection.grantAccess(username, permissions);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public void resetAccess(final String username) {
		try {
			collection.resetAccess(username);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	@Override
	public Permissions getPermissions(final String username) throws DataAccessException {
		try {
			return collection.getPermissions(username);
		} catch (final ArangoDBException e) {
			throw translateException(e);
		}
	}

	private RuntimeException translateException(RuntimeException e) {
		return DataAccessUtils.translateIfNecessary(e, exceptionTranslator);
	}

}
