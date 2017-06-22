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

import com.arangodb.ArangoCollection;
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
import com.arangodb.springframework.core.ArangoCollectionOperations;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoCollectionTemplate extends ArangoTemplateBase implements ArangoCollectionOperations {

	private final ArangoCollection collection;

	protected ArangoCollectionTemplate(final ArangoCollection collection,
		final PersistenceExceptionTranslator exceptionTranslator) {
		super(exceptionTranslator);
		this.collection = collection;
	}

	@Override
	public <T> DocumentCreateEntity<T> insertDocument(final T value) throws DataAccessException {
		try {
			return collection.insertDocument(value);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> DocumentCreateEntity<T> insertDocument(final T value, final DocumentCreateOptions options)
			throws DataAccessException {
		try {
			return collection.insertDocument(value, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> MultiDocumentEntity<DocumentCreateEntity<T>> insertDocuments(final Collection<T> values)
			throws DataAccessException {
		try {
			return collection.insertDocuments(values);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> MultiDocumentEntity<DocumentCreateEntity<T>> insertDocuments(
		final Collection<T> values,
		final DocumentCreateOptions options) throws DataAccessException {
		try {
			return collection.insertDocuments(values, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public DocumentImportEntity importDocuments(final Collection<?> values) throws DataAccessException {
		try {
			return collection.importDocuments(values);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public DocumentImportEntity importDocuments(final Collection<?> values, final DocumentImportOptions options)
			throws DataAccessException {
		try {
			return collection.importDocuments(values, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public DocumentImportEntity importDocuments(final String values) throws DataAccessException {
		try {
			return collection.importDocuments(values);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public DocumentImportEntity importDocuments(final String values, final DocumentImportOptions options)
			throws DataAccessException {
		try {
			return collection.importDocuments(values, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> T getDocument(final String key, final Class<T> type) throws DataAccessException {
		try {
			return collection.getDocument(key, type);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> T getDocument(final String key, final Class<T> type, final DocumentReadOptions options)
			throws DataAccessException {
		try {
			return collection.getDocument(key, type, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> DocumentUpdateEntity<T> replaceDocument(final String key, final T value) throws DataAccessException {
		try {
			return collection.replaceDocument(key, value);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> DocumentUpdateEntity<T> replaceDocument(
		final String key,
		final T value,
		final DocumentReplaceOptions options) throws DataAccessException {
		try {
			return collection.replaceDocument(key, value, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> MultiDocumentEntity<DocumentUpdateEntity<T>> replaceDocuments(final Collection<T> values)
			throws DataAccessException {
		try {
			return collection.replaceDocuments(values);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> MultiDocumentEntity<DocumentUpdateEntity<T>> replaceDocuments(
		final Collection<T> values,
		final DocumentReplaceOptions options) throws DataAccessException {
		try {
			return collection.replaceDocuments(values, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> DocumentUpdateEntity<T> updateDocument(final String key, final T value) throws DataAccessException {
		try {
			return collection.updateDocument(key, value);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> DocumentUpdateEntity<T> updateDocument(
		final String key,
		final T value,
		final DocumentUpdateOptions options) throws DataAccessException {
		try {
			return collection.updateDocument(key, value, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> MultiDocumentEntity<DocumentUpdateEntity<T>> updateDocuments(final Collection<T> values)
			throws DataAccessException {
		try {
			return collection.updateDocuments(values);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> MultiDocumentEntity<DocumentUpdateEntity<T>> updateDocuments(
		final Collection<T> values,
		final DocumentUpdateOptions options) throws DataAccessException {
		try {
			return collection.updateDocuments(values, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public DocumentDeleteEntity<Void> deleteDocument(final String key) throws DataAccessException {
		try {
			return collection.deleteDocument(key);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> DocumentDeleteEntity<T> deleteDocument(
		final String key,
		final Class<T> type,
		final DocumentDeleteOptions options) throws DataAccessException {
		try {
			return collection.deleteDocument(key, type, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public MultiDocumentEntity<DocumentDeleteEntity<Void>> deleteDocuments(final Collection<?> values)
			throws DataAccessException {
		try {
			return collection.deleteDocuments(values);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteDocuments(
		final Collection<?> values,
		final Class<T> type,
		final DocumentDeleteOptions options) throws DataAccessException {
		try {
			return collection.deleteDocuments(values, type, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Boolean documentExists(final String key) {
		try {
			return collection.documentExists(key);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Boolean documentExists(final String key, final DocumentExistsOptions options) {
		try {
			return collection.documentExists(key, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public IndexEntity getIndex(final String id) throws DataAccessException {
		try {
			return collection.getIndex(id);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public String deleteIndex(final String id) throws DataAccessException {
		try {
			return collection.deleteIndex(id);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public IndexEntity createHashIndex(final Collection<String> fields, final HashIndexOptions options)
			throws DataAccessException {
		try {
			return collection.createHashIndex(fields, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public IndexEntity createSkiplistIndex(final Collection<String> fields, final SkiplistIndexOptions options)
			throws DataAccessException {
		try {
			return collection.createSkiplistIndex(fields, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public IndexEntity createPersistentIndex(final Collection<String> fields, final PersistentIndexOptions options)
			throws DataAccessException {
		try {
			return collection.createPersistentIndex(fields, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public IndexEntity createGeoIndex(final Collection<String> fields, final GeoIndexOptions options)
			throws DataAccessException {
		try {
			return collection.createGeoIndex(fields, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public IndexEntity createFulltextIndex(final Collection<String> fields, final FulltextIndexOptions options)
			throws DataAccessException {
		try {
			return collection.createFulltextIndex(fields, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Collection<IndexEntity> getIndexes() throws DataAccessException {
		try {
			return collection.getIndexes();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public CollectionEntity truncate() throws DataAccessException {
		try {
			return collection.truncate();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public CollectionPropertiesEntity count() throws DataAccessException {
		try {
			return collection.count();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
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
	public void drop(final boolean isSystem) throws DataAccessException {
		try {
			collection.drop(isSystem);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public CollectionEntity load() throws DataAccessException {
		try {
			return collection.load();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public CollectionEntity unload() throws DataAccessException {
		try {
			return collection.unload();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public CollectionEntity getInfo() throws DataAccessException {
		try {
			return collection.getInfo();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public CollectionPropertiesEntity getProperties() throws DataAccessException {
		try {
			return collection.getProperties();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public CollectionPropertiesEntity changeProperties(final CollectionPropertiesOptions options)
			throws DataAccessException {
		try {
			return collection.changeProperties(options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public CollectionEntity rename(final String newName) throws DataAccessException {
		try {
			return collection.rename(newName);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public CollectionRevisionEntity getRevision() throws DataAccessException {
		try {
			return collection.getRevision();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

}
