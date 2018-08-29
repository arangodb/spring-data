/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
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

import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import com.arangodb.ArangoDBException;
import com.arangodb.ArangoSearch;
import com.arangodb.entity.arangosearch.ArangoSearchPropertiesEntity;
import com.arangodb.model.arangosearch.ArangoSearchPropertiesOptions;
import com.arangodb.springframework.core.ArangoSearchOperations;

/**
 * @author Mark Vollmary
 *
 */
public class DefaultArangoSearchOperations implements ArangoSearchOperations {

	private final ArangoSearch view;
	private final Map<CollectionCacheKey, ArangoSearchCacheValue> viewCache;
	private final PersistenceExceptionTranslator exceptionTranslator;

	protected DefaultArangoSearchOperations(final ArangoSearch view,
		final Map<CollectionCacheKey, ArangoSearchCacheValue> viewCache,
		final PersistenceExceptionTranslator exceptionTranslator) {
		this.view = view;
		this.viewCache = viewCache;
		this.exceptionTranslator = exceptionTranslator;
	}

	private DataAccessException translateExceptionIfPossible(final RuntimeException exception) {
		return exceptionTranslator.translateExceptionIfPossible(exception);
	}

	@Override
	public String name() {
		return view.name();
	}

	@Override
	public void drop() throws DataAccessException {
		viewCache.remove(new CollectionCacheKey(view.db().name(), view.name()));
		try {
			view.drop();
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public ArangoSearchPropertiesEntity getProperties() throws DataAccessException {
		try {
			return view.getProperties();
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public ArangoSearchPropertiesEntity updateProperties(final ArangoSearchPropertiesOptions options)
			throws DataAccessException {
		try {
			return view.updateProperties(options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public ArangoSearchPropertiesEntity replaceProperties(final ArangoSearchPropertiesOptions options)
			throws DataAccessException {
		try {
			return view.replaceProperties(options);
		} catch (final ArangoDBException e) {
			throw translateExceptionIfPossible(e);
		}
	}

}
