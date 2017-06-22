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

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import com.arangodb.ArangoVertexCollection;
import com.arangodb.entity.VertexEntity;
import com.arangodb.entity.VertexUpdateEntity;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.VertexCreateOptions;
import com.arangodb.model.VertexDeleteOptions;
import com.arangodb.model.VertexReplaceOptions;
import com.arangodb.model.VertexUpdateOptions;
import com.arangodb.springframework.core.ArangoVertexCollectionOperations;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoVertexCollectionTemplate extends ArangoTemplateBase implements ArangoVertexCollectionOperations {

	private final ArangoVertexCollection collection;

	protected ArangoVertexCollectionTemplate(final ArangoVertexCollection collection,
		final PersistenceExceptionTranslator exceptionTranslator) {
		super(exceptionTranslator);
		this.collection = collection;
	}

	@Override
	public void drop() throws DataAccessException {
		try {
			collection.drop();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> VertexEntity insertVertex(final T value) throws DataAccessException {
		try {
			return collection.insertVertex(value);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> VertexEntity insertVertex(final T value, final VertexCreateOptions options) throws DataAccessException {
		try {
			return collection.insertVertex(value, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> T getVertex(final String key, final Class<T> type) throws DataAccessException {
		try {
			return collection.getVertex(key, type);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> T getVertex(final String key, final Class<T> type, final DocumentReadOptions options)
			throws DataAccessException {
		try {
			return collection.getVertex(key, type, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> VertexUpdateEntity replaceVertex(final String key, final T value) throws DataAccessException {
		try {
			return collection.replaceVertex(key, value);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> VertexUpdateEntity replaceVertex(final String key, final T value, final VertexReplaceOptions options)
			throws DataAccessException {
		try {
			return collection.replaceVertex(key, value, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> VertexUpdateEntity updateVertex(final String key, final T value) throws DataAccessException {
		try {
			return collection.updateVertex(key, value);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> VertexUpdateEntity updateVertex(final String key, final T value, final VertexUpdateOptions options)
			throws DataAccessException {
		try {
			return collection.updateVertex(key, value, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public void deleteVertex(final String key) throws DataAccessException {
		try {
			collection.deleteVertex(key);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public void deleteVertex(final String key, final VertexDeleteOptions options) throws DataAccessException {
		try {
			collection.deleteVertex(key, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

}
