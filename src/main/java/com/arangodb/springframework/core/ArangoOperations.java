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
import com.arangodb.entity.UserEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.DocumentReplaceOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.springframework.core.convert.ArangoConverter;

/**
 * @author Mark Vollmary
 *
 */
public interface ArangoOperations {

	ArangoDB driver();

	ArangoDBVersion getVersion() throws DataAccessException;

	<T> ArangoCursor<T> query(String query, Map<String, Object> bindVars, AqlQueryOptions options, Class<T> entityClass)
			throws DataAccessException;

	<T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteDocuments(
		Collection<Object> values,
		Class<T> entityClass,
		DocumentDeleteOptions options) throws DataAccessException;

	<T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteDocuments(Collection<Object> values, Class<T> entityClass)
			throws DataAccessException;

	<T> DocumentDeleteEntity<T> deleteDocument(String id, Class<T> entityClass, DocumentDeleteOptions options)
			throws DataAccessException;

	<T> DocumentDeleteEntity<Void> deleteDocument(String id, Class<T> entityClass) throws DataAccessException;

	MultiDocumentEntity<DocumentUpdateEntity<Object>> updateDocuments(
		Collection<Object> values,
		Class<?> entityClass,
		DocumentUpdateOptions options) throws DataAccessException;

	MultiDocumentEntity<DocumentUpdateEntity<Object>> updateDocuments(Collection<Object> values, Class<?> entityClass)
			throws DataAccessException;

	<T> DocumentUpdateEntity<T> updateDocument(String id, T value, DocumentUpdateOptions options)
			throws DataAccessException;

	<T> DocumentUpdateEntity<T> updateDocument(String id, T value) throws DataAccessException;

	MultiDocumentEntity<DocumentUpdateEntity<Object>> replaceDocuments(
		Collection<Object> values,
		Class<?> entityClass,
		DocumentReplaceOptions options) throws DataAccessException;

	MultiDocumentEntity<DocumentUpdateEntity<Object>> replaceDocuments(Collection<Object> values, Class<?> entityClass)
			throws DataAccessException;

	<T> DocumentUpdateEntity<T> replaceDocument(String id, T value, DocumentReplaceOptions options)
			throws DataAccessException;

	<T> DocumentUpdateEntity<T> replaceDocument(String id, T value) throws DataAccessException;

	<T> T getDocument(String id, Class<T> entityClass, DocumentReadOptions options) throws DataAccessException;

	<T> T getDocument(String id, Class<T> entityClass) throws DataAccessException;

	<T> Iterable<T> getDocuments(Class<T> entityClass) throws DataAccessException;

	<T> Iterable<T> getDocuments(final Iterable<String> ids, final Class<T> entityClass) throws DataAccessException;

	MultiDocumentEntity<DocumentCreateEntity<Object>> insertDocuments(
		Collection<Object> values,
		Class<?> entityClass,
		DocumentCreateOptions options) throws DataAccessException;

	MultiDocumentEntity<DocumentCreateEntity<Object>> insertDocuments(Collection<Object> values, Class<?> entityClass)
			throws DataAccessException;

	<T> DocumentCreateEntity<T> insertDocument(T value, DocumentCreateOptions options) throws DataAccessException;

	<T> DocumentCreateEntity<T> insertDocument(T value) throws DataAccessException;

	boolean exists(String id, Class<?> entityClass) throws DataAccessException;

	void dropDatabase() throws DataAccessException;

	CollectionOperations collection(Class<?> entityClass) throws DataAccessException;

	CollectionOperations collection(String name) throws DataAccessException;

	CollectionOperations collection(String name, CollectionCreateOptions options) throws DataAccessException;

	UserOperations user(String username);

	Collection<UserEntity> getUsers() throws DataAccessException;

	ArangoConverter getConverter();

}
