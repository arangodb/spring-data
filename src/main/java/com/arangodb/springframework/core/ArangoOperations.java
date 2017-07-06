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

import java.util.Collection;
import java.util.Map;

import org.springframework.dao.DataAccessException;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.DocumentReplaceOptions;
import com.arangodb.model.DocumentUpdateOptions;

/**
 * @author Mark Vollmary
 *
 */
public interface ArangoOperations {

	ArangoDB driver();

	ArangoDBVersion getVersion() throws DataAccessException;

	<T> ArangoCursor<T> query(
		final String query,
		final Map<String, Object> bindVars,
		final AqlQueryOptions options,
		final Class<T> type) throws DataAccessException;

	<T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteDocuments(
		final Collection<Object> values,
		final Class<T> type,
		final DocumentDeleteOptions options) throws DataAccessException;

	<T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteDocuments(final Collection<Object> values, Class<T> type)
			throws DataAccessException;

	<T> DocumentDeleteEntity<T> deleteDocument(
		final String id,
		final Class<T> type,
		final DocumentDeleteOptions options) throws DataAccessException;

	<T> DocumentDeleteEntity<Void> deleteDocument(final String id, Class<T> type) throws DataAccessException;

	MultiDocumentEntity<DocumentUpdateEntity<Object>> updateDocuments(
		final Collection<Object> values,
		Class<?> type,
		final DocumentUpdateOptions options) throws DataAccessException;

	MultiDocumentEntity<DocumentUpdateEntity<Object>> updateDocuments(final Collection<Object> values, Class<?> type)
			throws DataAccessException;

	<T> DocumentUpdateEntity<T> updateDocument(final String id, final T value, final DocumentUpdateOptions options)
			throws DataAccessException;

	<T> DocumentUpdateEntity<T> updateDocument(final String id, final T value) throws DataAccessException;

	MultiDocumentEntity<DocumentUpdateEntity<Object>> replaceDocuments(
		final Collection<Object> values,
		Class<?> type,
		final DocumentReplaceOptions options) throws DataAccessException;

	MultiDocumentEntity<DocumentUpdateEntity<Object>> replaceDocuments(final Collection<Object> values, Class<?> type)
			throws DataAccessException;

	<T> DocumentUpdateEntity<T> replaceDocument(final String id, final T value, final DocumentReplaceOptions options)
			throws DataAccessException;

	<T> DocumentUpdateEntity<T> replaceDocument(final String id, final T value) throws DataAccessException;

	<T> T getDocument(final String id, final Class<T> type, final DocumentReadOptions options)
			throws DataAccessException;

	<T> T getDocument(final String id, final Class<T> type) throws DataAccessException;

	MultiDocumentEntity<DocumentCreateEntity<Object>> insertDocuments(
		final Collection<Object> values,
		Class<?> type,
		final DocumentCreateOptions options) throws DataAccessException;

	MultiDocumentEntity<DocumentCreateEntity<Object>> insertDocuments(final Collection<Object> values, Class<?> type)
			throws DataAccessException;

	<T> DocumentCreateEntity<T> insertDocument(final T value, final DocumentCreateOptions options)
			throws DataAccessException;

	<T> DocumentCreateEntity<T> insertDocument(final T value) throws DataAccessException;

	void dropCollection(Class<?> type);

	void dropDatabase();
}
