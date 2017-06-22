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

import com.arangodb.ArangoEdgeCollection;
import com.arangodb.entity.EdgeEntity;
import com.arangodb.entity.EdgeUpdateEntity;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.EdgeCreateOptions;
import com.arangodb.model.EdgeDeleteOptions;
import com.arangodb.model.EdgeReplaceOptions;
import com.arangodb.model.EdgeUpdateOptions;
import com.arangodb.springframework.core.ArangoEdgeCollectionOperations;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoEdgeCollectionTemplate extends ArangoTemplateBase implements ArangoEdgeCollectionOperations {

	private final ArangoEdgeCollection collection;

	protected ArangoEdgeCollectionTemplate(final ArangoEdgeCollection collection,
		final PersistenceExceptionTranslator exceptionTranslator) {
		super(exceptionTranslator);
		this.collection = collection;
	}

	@Override
	public <T> EdgeEntity insertEdge(final T value) throws DataAccessException {
		try {
			return collection.insertEdge(value);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> EdgeEntity insertEdge(final T value, final EdgeCreateOptions options) throws DataAccessException {
		try {
			return collection.insertEdge(value, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> T getEdge(final String key, final Class<T> type) throws DataAccessException {
		try {
			return collection.getEdge(key, type);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> T getEdge(final String key, final Class<T> type, final DocumentReadOptions options)
			throws DataAccessException {
		try {
			return collection.getEdge(key, type, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> EdgeUpdateEntity replaceEdge(final String key, final T value) throws DataAccessException {
		try {
			return collection.replaceEdge(key, value);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> EdgeUpdateEntity replaceEdge(final String key, final T value, final EdgeReplaceOptions options)
			throws DataAccessException {
		try {
			return collection.replaceEdge(key, value, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> EdgeUpdateEntity updateEdge(final String key, final T value) throws DataAccessException {
		try {
			return collection.updateEdge(key, value);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> EdgeUpdateEntity updateEdge(final String key, final T value, final EdgeUpdateOptions options)
			throws DataAccessException {
		try {
			return collection.updateEdge(key, value, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public void deleteEdge(final String key) throws DataAccessException {
		try {
			collection.deleteEdge(key);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public void deleteEdge(final String key, final EdgeDeleteOptions options) throws DataAccessException {
		try {
			collection.deleteEdge(key, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

}
