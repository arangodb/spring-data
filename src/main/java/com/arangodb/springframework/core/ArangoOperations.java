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
import java.util.Map;

import org.springframework.dao.DataAccessException;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentImportEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.EdgeEntity;
import com.arangodb.entity.EdgeUpdateEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.entity.VertexEntity;
import com.arangodb.entity.VertexUpdateEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentImportOptions;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.DocumentReplaceOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.model.EdgeCreateOptions;
import com.arangodb.model.EdgeDeleteOptions;
import com.arangodb.model.EdgeReplaceOptions;
import com.arangodb.model.EdgeUpdateOptions;
import com.arangodb.model.VertexCreateOptions;
import com.arangodb.model.VertexDeleteOptions;
import com.arangodb.model.VertexReplaceOptions;
import com.arangodb.model.VertexUpdateOptions;

/**
 * @author Mark - mark at arangodb.com
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
		final Collection<?> values,
		final Class<T> type,
		final DocumentDeleteOptions options) throws DataAccessException;

	MultiDocumentEntity<DocumentDeleteEntity<Void>> deleteDocuments(final Collection<?> values)
			throws DataAccessException;

	<T> DocumentDeleteEntity<T> deleteDocument(
		final String key,
		final Class<T> type,
		final DocumentDeleteOptions options) throws DataAccessException;

	DocumentDeleteEntity<Void> deleteDocument(final String key) throws DataAccessException;

	<T> MultiDocumentEntity<DocumentUpdateEntity<T>> updateDocuments(
		final Collection<T> values,
		final DocumentUpdateOptions options) throws DataAccessException;

	<T> MultiDocumentEntity<DocumentUpdateEntity<T>> updateDocuments(final Collection<T> values)
			throws DataAccessException;

	<T> DocumentUpdateEntity<T> updateDocument(final String key, final T value, final DocumentUpdateOptions options)
			throws DataAccessException;

	<T> DocumentUpdateEntity<T> updateDocument(final String key, final T value) throws DataAccessException;

	<T> MultiDocumentEntity<DocumentUpdateEntity<T>> replaceDocuments(
		final Collection<T> values,
		final DocumentReplaceOptions options) throws DataAccessException;

	<T> MultiDocumentEntity<DocumentUpdateEntity<T>> replaceDocuments(final Collection<T> values)
			throws DataAccessException;

	<T> DocumentUpdateEntity<T> replaceDocument(final String key, final T value, final DocumentReplaceOptions options)
			throws DataAccessException;

	<T> DocumentUpdateEntity<T> replaceDocument(final String key, final T value) throws DataAccessException;

	<T> T getDocument(final String key, final Class<T> type, final DocumentReadOptions options)
			throws DataAccessException;

	<T> T getDocument(final String key, final Class<T> type) throws DataAccessException;

	DocumentImportEntity importDocuments(final String values, final DocumentImportOptions options)
			throws DataAccessException;

	DocumentImportEntity importDocuments(final String values) throws DataAccessException;

	DocumentImportEntity importDocuments(final Collection<?> values, final DocumentImportOptions options)
			throws DataAccessException;

	DocumentImportEntity importDocuments(final Collection<?> values) throws DataAccessException;

	<T> MultiDocumentEntity<DocumentCreateEntity<T>> insertDocuments(
		final Collection<T> values,
		final DocumentCreateOptions options) throws DataAccessException;

	<T> MultiDocumentEntity<DocumentCreateEntity<T>> insertDocuments(final Collection<T> values)
			throws DataAccessException;

	<T> DocumentCreateEntity<T> insertDocument(final T value, final DocumentCreateOptions options)
			throws DataAccessException;

	<T> DocumentCreateEntity<T> insertDocument(final T value) throws DataAccessException;

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

	void deleteEdge(final String key, final EdgeDeleteOptions options) throws DataAccessException;

	void deleteEdge(final String key) throws DataAccessException;

	<T> EdgeUpdateEntity updateEdge(final String key, final T value, final EdgeUpdateOptions options)
			throws DataAccessException;

	<T> EdgeUpdateEntity updateEdge(final String key, final T value) throws DataAccessException;

	<T> EdgeUpdateEntity replaceEdge(final String key, final T value, final EdgeReplaceOptions options)
			throws DataAccessException;

	<T> EdgeUpdateEntity replaceEdge(final String key, final T value) throws DataAccessException;

	<T> T getEdge(final String key, final Class<T> type, final DocumentReadOptions options) throws DataAccessException;

	<T> T getEdge(final String key, final Class<T> type) throws DataAccessException;

	<T> EdgeEntity insertEdge(final T value, final EdgeCreateOptions options) throws DataAccessException;

	<T> EdgeEntity insertEdge(final T value) throws DataAccessException;

}
