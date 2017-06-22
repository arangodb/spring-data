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

import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionPropertiesEntity;
import com.arangodb.entity.CollectionRevisionEntity;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentImportEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.IndexEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.model.CollectionPropertiesOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentExistsOptions;
import com.arangodb.model.DocumentImportOptions;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.DocumentReplaceOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.model.FulltextIndexOptions;
import com.arangodb.model.GeoIndexOptions;
import com.arangodb.model.HashIndexOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.arangodb.model.SkiplistIndexOptions;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public interface ArangoCollectionOperations {

	CollectionRevisionEntity getRevision() throws DataAccessException;

	CollectionEntity rename(final String newName) throws DataAccessException;

	CollectionPropertiesEntity changeProperties(final CollectionPropertiesOptions options) throws DataAccessException;

	CollectionPropertiesEntity getProperties() throws DataAccessException;

	CollectionEntity getInfo() throws DataAccessException;

	CollectionEntity unload() throws DataAccessException;

	CollectionEntity load() throws DataAccessException;

	void drop(final boolean isSystem) throws DataAccessException;

	void drop() throws DataAccessException;

	CollectionPropertiesEntity count() throws DataAccessException;

	CollectionEntity truncate() throws DataAccessException;

	Collection<IndexEntity> getIndexes() throws DataAccessException;

	IndexEntity createFulltextIndex(final Collection<String> fields, final FulltextIndexOptions options)
			throws DataAccessException;

	IndexEntity createGeoIndex(final Collection<String> fields, final GeoIndexOptions options)
			throws DataAccessException;

	IndexEntity createPersistentIndex(final Collection<String> fields, final PersistentIndexOptions options)
			throws DataAccessException;

	IndexEntity createSkiplistIndex(final Collection<String> fields, final SkiplistIndexOptions options)
			throws DataAccessException;

	IndexEntity createHashIndex(final Collection<String> fields, final HashIndexOptions options)
			throws DataAccessException;

	String deleteIndex(final String id) throws DataAccessException;

	IndexEntity getIndex(final String id) throws DataAccessException;

	Boolean documentExists(final String key, final DocumentExistsOptions options);

	Boolean documentExists(final String key);

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

}
