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

import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataAccessException;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.DocumentEntity;
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

	/**
	 * Give direct access to the underlying driver
	 * 
	 * @return main access object of the driver
	 */
	ArangoDB driver();

	/**
	 * Returns the server name and version number.
	 * 
	 * @return the server version, number
	 * @throws DataAccessException
	 */
	ArangoDBVersion getVersion() throws DataAccessException;

	/**
	 * Create a cursor and return the first results
	 * 
	 * @param query
	 *            contains the query string to be executed
	 * @param bindVars
	 *            key/value pairs representing the bind parameters, can be null
	 * @param options
	 *            Additional options, can be null
	 * @param entityClass
	 *            The entity type of the result
	 * @return cursor of the results
	 * @throws DataAccessException
	 */
	<T> ArangoCursor<T> query(String query, Map<String, Object> bindVars, AqlQueryOptions options, Class<T> entityClass)
			throws DataAccessException;

	/**
	 * Create a cursor and return the first results. For queries without bind parameters.
	 * 
	 * @param query
	 *            contains the query string to be executed
	 * @param options
	 *            Additional options, can be null
	 * @param entityClass
	 *            The entity type of the result
	 * @return cursor of the results
	 * @throws DataAccessException
	 */
	<T> ArangoCursor<T> query(String query, AqlQueryOptions options, Class<T> entityClass) throws DataAccessException;

	/**
	 * Removes multiple document
	 * 
	 * @param values
	 *            The keys of the documents or the documents themselves
	 * @param entityClass
	 *            The entity type of the documents
	 * @param options
	 *            Additional options, can be null
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	MultiDocumentEntity<? extends DocumentEntity> delete(
		Iterable<Object> values,
		Class<?> entityClass,
		DocumentDeleteOptions options) throws DataAccessException;

	/**
	 * Removes multiple document
	 * 
	 * @param values
	 *            The keys of the documents or the documents themselves
	 * @param entityClass
	 *            The entity type of the documents
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	MultiDocumentEntity<? extends DocumentEntity> delete(Iterable<Object> values, Class<?> entityClass)
			throws DataAccessException;

	/**
	 * Removes a document
	 * 
	 * @param id
	 *            The id or key of the document
	 * @param entityClass
	 *            The entity type of the document
	 * @param options
	 *            Additional options, can be null
	 * @return information about the document
	 * @throws DataAccessException
	 */
	DocumentEntity delete(String id, Class<?> entityClass, DocumentDeleteOptions options) throws DataAccessException;

	/**
	 * Removes a document
	 * 
	 * @param id
	 *            The id or key of the document
	 * @param entityClass
	 *            The entity type of the document
	 * @return information about the document
	 * @throws DataAccessException
	 */
	DocumentEntity delete(String id, Class<?> entityClass) throws DataAccessException;

	/**
	 * Partially updates documents, the documents to update are specified by the _key attributes in the objects on
	 * values. Vales must contain a list of document updates with the attributes to patch (the patch documents). All
	 * attributes from the patch documents will be added to the existing documents if they do not yet exist, and
	 * overwritten in the existing documents if they do exist there.
	 * 
	 * @param <T>
	 * 
	 * @param values
	 *            A list of documents
	 * @param entityClass
	 *            The entity type of the documents
	 * @param options
	 *            Additional options, can be null
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	<T> MultiDocumentEntity<? extends DocumentEntity> update(
		Iterable<T> values,
		Class<T> entityClass,
		DocumentUpdateOptions options) throws DataAccessException;

	/**
	 * Partially updates documents, the documents to update are specified by the _key attributes in the objects on
	 * values. Vales must contain a list of document updates with the attributes to patch (the patch documents). All
	 * attributes from the patch documents will be added to the existing documents if they do not yet exist, and
	 * overwritten in the existing documents if they do exist there.
	 * 
	 * @param <T>
	 * 
	 * @param values
	 *            A list of documents
	 * @param entityClass
	 *            The entity type of the documents
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	<T> MultiDocumentEntity<? extends DocumentEntity> update(Iterable<T> values, Class<T> entityClass)
			throws DataAccessException;

	/**
	 * Partially updates the document identified by document id or key. The value must contain a document with the
	 * attributes to patch (the patch document). All attributes from the patch document will be added to the existing
	 * document if they do not yet exist, and overwritten in the existing document if they do exist there.
	 * 
	 * @param id
	 *            The id or key of the document
	 * @param value
	 *            A representation of a single document
	 * @param options
	 *            Additional options, can be null
	 * @return information about the document
	 * @throws DataAccessException
	 */
	<T> DocumentEntity update(String id, T value, DocumentUpdateOptions options) throws DataAccessException;

	/**
	 * Partially updates the document identified by document id or key. The value must contain a document with the
	 * attributes to patch (the patch document). All attributes from the patch document will be added to the existing
	 * document if they do not yet exist, and overwritten in the existing document if they do exist there.
	 * 
	 * @param id
	 *            The id or key of the document
	 * @param value
	 *            A representation of a single document
	 * @return information about the document
	 * @throws DataAccessException
	 */
	<T> DocumentEntity update(String id, T value) throws DataAccessException;

	/**
	 * Replaces multiple documents in the specified collection with the ones in the values, the replaced documents are
	 * specified by the _key attributes in the documents in values.
	 * 
	 * @param <T>
	 * 
	 * @param values
	 *            A List of documents
	 * @param entityClass
	 *            The entity type of the documents
	 * @param options
	 *            Additional options, can be null
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	<T> MultiDocumentEntity<? extends DocumentEntity> replace(
		Iterable<T> values,
		Class<T> entityClass,
		DocumentReplaceOptions options) throws DataAccessException;

	/**
	 * Replaces multiple documents in the specified collection with the ones in the values, the replaced documents are
	 * specified by the _key attributes in the documents in values.
	 * 
	 * @param <T>
	 * 
	 * @param values
	 *            A List of documents
	 * @param entityClass
	 *            The entity type of the documents
	 * @param options
	 *            Additional options, can be null
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	<T> MultiDocumentEntity<? extends DocumentEntity> replace(Iterable<T> values, Class<T> entityClass)
			throws DataAccessException;

	/**
	 * Replaces the document with key with the one in the body, provided there is such a document and no precondition is
	 * violated
	 * 
	 * @param id
	 *            The id or key of the document
	 * @param value
	 *            A representation of a single document (POJO, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return information about the document
	 * @throws DataAccessException
	 */
	<T> DocumentEntity replace(String id, T value, DocumentReplaceOptions options) throws DataAccessException;

	/**
	 * Replaces the document with key with the one in the body, provided there is such a document and no precondition is
	 * violated
	 * 
	 * @param id
	 *            The id or key of the document
	 * @param value
	 *            A representation of a single document (POJO, VPackSlice or String for Json)
	 * @return information about the document
	 * @throws DataAccessException
	 */
	<T> DocumentEntity replace(String id, T value) throws DataAccessException;

	<T> Optional<T> find(String id, Class<T> entityClass, DocumentReadOptions options) throws DataAccessException;

	<T> Optional<T> find(String id, Class<T> entityClass) throws DataAccessException;

	/**
	 * Reads all documents from a collection
	 * 
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @return the documents
	 * @throws DataAccessException
	 */
	<T> Iterable<T> findAll(Class<T> entityClass) throws DataAccessException;

	/**
	 * Reads multiple documents
	 * 
	 * @param ids
	 *            The ids or keys of the documents
	 * @param entityClass
	 *            The entity type of the documents
	 * @return the documents
	 * @throws DataAccessException
	 */
	<T> Iterable<T> find(final Iterable<String> ids, final Class<T> entityClass) throws DataAccessException;

	/**
	 * Creates new documents from the given documents, unless there is already a document with the _key given. If no
	 * _key is given, a new unique _key is generated automatically.
	 * 
	 * @param <T>
	 * 
	 * @param values
	 *            A List of documents
	 * @param entityClass
	 *            The entity type of the documents
	 * @param options
	 *            Additional options, can be null
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	<T> MultiDocumentEntity<? extends DocumentEntity> insert(
		Iterable<T> values,
		Class<T> entityClass,
		DocumentCreateOptions options) throws DataAccessException;

	/**
	 * Creates new documents from the given documents, unless there is already a document with the _key given. If no
	 * _key is given, a new unique _key is generated automatically.
	 * 
	 * @param <T>
	 * 
	 * @param values
	 *            A List of documents
	 * @param entityClass
	 *            The entity type of the documents
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	<T> MultiDocumentEntity<? extends DocumentEntity> insert(Iterable<T> values, Class<T> entityClass)
			throws DataAccessException;

	/**
	 * Creates a new document from the given document, unless there is already a document with the _key given. If no
	 * _key is given, a new unique _key is generated automatically.
	 * 
	 * @param value
	 *            A representation of a single document
	 * @param options
	 *            Additional options, can be null
	 * @return information about the document
	 */
	<T> DocumentEntity insert(T value, DocumentCreateOptions options) throws DataAccessException;

	/**
	 * Creates a new document from the given document, unless there is already a document with the _key given. If no
	 * _key is given, a new unique _key is generated automatically.
	 * 
	 * @param value
	 *            A representation of a single document
	 * @return information about the document
	 */
	<T> DocumentEntity insert(T value) throws DataAccessException;

	/**
	 * Creates a new document from the given document, unless there is already a document with the _key given. If no
	 * _key is given, a new unique _key is generated automatically.
	 * 
	 * @param collectionName
	 *            Name of the collection in which the new document should be inserted
	 * @param value
	 *            A representation of a single document
	 * @param options
	 *            Additional options, can be null
	 * @return information about the document
	 * @throws DataAccessException
	 */
	DocumentEntity insert(String collectionName, Object value, DocumentCreateOptions options)
			throws DataAccessException;

	/**
	 * 
	 * Creates a new document from the given document, unless there is already a document with the _key given. If no
	 * _key is given, a new unique _key is generated automatically.
	 * 
	 * @param collectionName
	 *            Name of the collection in which the new document should be inserted
	 * @param value
	 *            A representation of a single document
	 * @return information about the document
	 * @throws DataAccessException
	 */
	DocumentEntity insert(String collectionName, Object value) throws DataAccessException;

	public enum UpsertStrategy {
		REPLACE, UPDATE
	}

	/**
	 * Creates a new document from the given document, unless there is already a document with the id given. In that
	 * case it updates or replaces the document, depending on the chosen strategy.
	 * 
	 * @deprecated use {@link #repsert(Object)} instead
	 * @param value
	 *            A representation of a single document
	 * @param strategy
	 *            The strategy to use when not inserting the document
	 * @throws DataAccessException
	 */
	@Deprecated
	<T> void upsert(T value, UpsertStrategy strategy) throws DataAccessException;

	/**
	 * Creates new documents from the given documents, unless there already exists. In that case it updates or replaces
	 * the documents, depending on the chosen strategy.
	 * 
	 * @deprecated use {@link #repsert(Iterable)} instead
	 * @param value
	 *            A List of documents
	 * @param strategy
	 *            The strategy to use when not inserting the document
	 * @throws DataAccessException
	 */
	@Deprecated
	<T> void upsert(Iterable<T> value, UpsertStrategy strategy) throws DataAccessException;

	/**
	 * Creates a new document from the given document, unless there is already a document with the id given. In that
	 * case it replaces the document.
	 * 
	 * @param value
	 *            A representation of a single document
	 * @throws DataAccessException
	 * @since ArangoDB 3.4
	 */
	<T> void repsert(T value) throws DataAccessException;

	/**
	 * Creates new documents from the given documents, unless there already exists. In that case it replaces the
	 * documents.
	 * 
	 * @param value
	 *            A List of documents
	 * @param entityClass
	 *            The entity type of the documents
	 * @throws DataAccessException
	 * @since ArangoDB 3.4
	 */
	<T> void repsert(Iterable<T> value, Class<T> entityClass) throws DataAccessException;

	/**
	 * Checks whether the document exists by reading a single document head
	 * 
	 * @param id
	 *            The id or key of the document
	 * @param entityClass
	 *            The entity type of the document
	 * @return true if the document exists, false if not
	 * @throws DataAccessException
	 */
	boolean exists(String id, Class<?> entityClass) throws DataAccessException;

	/**
	 * Drop an existing database
	 * 
	 * @throws DataAccessException
	 */
	void dropDatabase() throws DataAccessException;

	/**
	 * Returns the operations interface for a collection. If the collection does not exists, it is created
	 * automatically.
	 * 
	 * @param entityClass
	 *            The entity type representing the collection
	 * @return {@link CollectionOperations}
	 * @throws DataAccessException
	 */
	CollectionOperations collection(Class<?> entityClass) throws DataAccessException;

	/**
	 * Returns the operations interface for a collection. If the collection does not exists, it is created
	 * automatically.
	 * 
	 * @param name
	 *            The name of the collection
	 * @return {@link CollectionOperations}
	 * @throws DataAccessException
	 */
	CollectionOperations collection(String name) throws DataAccessException;

	/**
	 * Returns the operations interface for a collection. If the collection does not exists, it is created
	 * automatically.
	 * 
	 * @param name
	 *            The name of the collection
	 * @param options
	 *            Additional options for collection creation, can be null
	 * @return {@link CollectionOperations}
	 * @throws DataAccessException
	 */
	CollectionOperations collection(String name, CollectionCreateOptions options) throws DataAccessException;

	/**
	 * Return the operations interface for a user. The user is not created automatically if it does not exists.
	 * 
	 * @param username
	 *            The name of the user
	 * @return {@link UserOperations}
	 */
	UserOperations user(String username);

	/**
	 * Fetches data about all users. You can only execute this call if you have access to the _system database.
	 * 
	 * @return informations about all users
	 * @throws DataAccessException
	 */
	Iterable<UserEntity> getUsers() throws DataAccessException;

	ArangoConverter getConverter();

}
