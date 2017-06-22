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

import org.springframework.dao.DataAccessException;

import com.arangodb.entity.VertexEntity;
import com.arangodb.entity.VertexUpdateEntity;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.VertexCreateOptions;
import com.arangodb.model.VertexDeleteOptions;
import com.arangodb.model.VertexReplaceOptions;
import com.arangodb.model.VertexUpdateOptions;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public interface ArangoVertexCollectionOperations {

	void deleteVertex(final String key, final VertexDeleteOptions options) throws DataAccessException;

	void deleteVertex(final String key) throws DataAccessException;

	<T> VertexUpdateEntity updateVertex(final String key, final T value, final VertexUpdateOptions options)
			throws DataAccessException;

	<T> VertexUpdateEntity updateVertex(final String key, final T value) throws DataAccessException;

	<T> VertexUpdateEntity replaceVertex(final String key, final T value, final VertexReplaceOptions options)
			throws DataAccessException;

	<T> VertexUpdateEntity replaceVertex(final String key, final T value) throws DataAccessException;

	<T> T getVertex(final String key, final Class<T> type, final DocumentReadOptions options)
			throws DataAccessException;

	<T> T getVertex(final String key, final Class<T> type) throws DataAccessException;

	<T> VertexEntity insertVertex(final T value, final VertexCreateOptions options) throws DataAccessException;

	<T> VertexEntity insertVertex(final T value) throws DataAccessException;

	void drop() throws DataAccessException;

}
