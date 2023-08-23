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

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.*;
import com.arangodb.model.*;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.resolver.ResolverFactory;
import org.springframework.dao.DataAccessException;

import java.util.Map;
import java.util.Optional;

/**
 * Interface that specifies a basic set of ArangoDB operations.
 *
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
	 * Returns the underlying database. The database will be created if it does not exist.
	 *
	 * @return the database object
	 * @throws DataAccessException
	 */
	ArangoDatabase db() throws DataAccessException;

	/**
	 * Performs a database query using the given {@code query} and {@code bindVars}, then returns a new
	 * {@code ArangoCursor} instance for the result list.
	 *
	 * @param query
	 *            An AQL query string
	 * @param bindVars
	 *            key/value pairs defining the variables to bind the query to
	 * @param options
	 *            Additional options that will be passed to the query API, can be null
	 * @param entityClass
	 *            The entity type of the result
	 * @return cursor of the results
	 * @throws DataAccessException
	 */
	<T> ArangoCursor<T> query(String query, Map<String, Object> bindVars, AqlQueryOptions options, Class<T> entityClass)
			throws DataAccessException;

	/**
	 * Performs a database query using the given {@code query} and {@code bindVars}, then returns a new
	 * {@code ArangoCursor} instance for the result list.
	 *
	 * @param query
	 *            An AQL query string
	 * @param bindVars
	 *            key/value pairs defining the variables to bind the query to
	 * @param entityClass
	 *            The entity type of the result
	 * @return cursor of the results
	 * @throws DataAccessException
	 */
	default <T> ArangoCursor<T> query(String query, Map<String, Object> bindVars, Class<T> entityClass)
			throws DataAccessException {
		return query(query, bindVars, new AqlQueryOptions(), entityClass);
	}

	/**
	 * Performs a database query using the given {@code query}, then returns a new {@code ArangoCursor}
	 * instance for the result list.
	 *
	 * @param query
	 *            An AQL query string
	 * @param entityClass
	 *            The entity type of the result
	 * @return cursor of the results
	 * @throws DataAccessException
	 */
    default <T> ArangoCursor<T> query(String query, AqlQueryOptions options, Class<T> entityClass)
            throws DataAccessException {
        return query(query, null, options, entityClass);
    }

	/**
	 * Performs a database query using the given {@code query}, then returns a new {@code ArangoCursor}
	 * instance for the result list.
	 *
	 * @param query
	 *            An AQL query string
	 * @param entityClass
	 *            The entity type of the result
	 * @return cursor of the results
	 * @throws DataAccessException
	 */
	default <T> ArangoCursor<T> query(String query, Class<T> entityClass) throws DataAccessException {
		return query(query, new AqlQueryOptions(), entityClass);
	}

	/**
	 * Deletes multiple documents from a collection.
	 *
	 * @param values
	 *            The keys of the documents or the documents themselves
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @param options
	 *            Additional options, can be null
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	<T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteAll(
		Iterable<?> values,
		DocumentDeleteOptions options,
		Class<T> entityClass) throws DataAccessException;

	/**
	 * Deletes multiple documents from a collection.
	 *
	 * @param values
	 *            The keys of the documents or the documents themselves
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	default <T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteAll(Iterable<?> values, Class<T> entityClass)
			throws DataAccessException {
		return deleteAll(values, new DocumentDeleteOptions(), entityClass);
	}

	/**
	 * Deletes multiple documents with the given IDs from a collection.
	 *
	 * @param ids
	 *            The keys of the documents
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @param options
	 *            Additional options, can be null
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	<T> MultiDocumentEntity<DocumentDeleteEntity<T>> deleteAllById(
			Iterable<?> ids,
			DocumentDeleteOptions options,
			Class<T> entityClass) throws DataAccessException;

	/**
	 * Deletes multiple documents with the given IDs from a collection.
	 *
	 * @param ids
	 *            The keys of the documents
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	MultiDocumentEntity<DocumentDeleteEntity<?>> deleteAllById(Iterable<?> ids, Class<?> entityClass) throws DataAccessException;

	/**
	 * Deletes the document with the given {@code id} from a collection.
	 *
	 * @param id
	 *            The id or key of the document
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @param options
	 *            Additional options, can be null
	 * @return information about the document
	 * @throws DataAccessException
	 */
	<T> DocumentDeleteEntity<T> delete(Object id, DocumentDeleteOptions options, Class<T> entityClass) throws DataAccessException;

	/**
	 * Deletes the document with the given {@code id} from a collection.
	 *
	 * @param id
	 *            The id or key of the document
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @return information about the document
	 * @throws DataAccessException
	 */
	default <T> DocumentDeleteEntity<T> delete(Object id, Class<T> entityClass) throws DataAccessException {
		return delete(id, new DocumentDeleteOptions(), entityClass);
	}

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
	 *            The entity class which represents the collection
	 * @param options
	 *            Additional options, can be null
	 * @return information about the documents
	 * @throws DataAccessException
	 */
    <T> MultiDocumentEntity<DocumentUpdateEntity<T>> updateAll(
            Iterable<? extends T> values,
            DocumentUpdateOptions options,
            Class<T> entityClass
    ) throws DataAccessException;

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
	 *            The entity class which represents the collection
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	default <T> MultiDocumentEntity<DocumentUpdateEntity<T>> updateAll(Iterable<T> values, Class<T> entityClass)
			throws DataAccessException {
		return updateAll(values, new DocumentUpdateOptions(), entityClass);
	}

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
	<T> DocumentUpdateEntity<T> update(Object id, T value, DocumentUpdateOptions options) throws DataAccessException;

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
	default <T> DocumentUpdateEntity<T> update(Object id, T value) throws DataAccessException {
		return update(id, value, new DocumentUpdateOptions());
	}

	/**
	 * Replaces multiple documents in the specified collection with the ones in the values, the replaced documents are
	 * specified by the _key attributes in the documents in values.
	 *
	 * @param <T>
	 *
	 * @param values
	 *            A List of documents
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @param options
	 *            Additional options, can be null
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	<T> MultiDocumentEntity<DocumentUpdateEntity<T>> replaceAll(
		Iterable<? extends T> values,
		DocumentReplaceOptions options,
		Class<T> entityClass) throws DataAccessException;

	/**
	 * Replaces multiple documents in the specified collection with the ones in the values, the replaced documents are
	 * specified by the _key attributes in the documents in values.
	 *
	 * @param <T>
	 *
	 * @param values
	 *            A List of documents
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	default <T> MultiDocumentEntity<DocumentUpdateEntity<T>> replaceAll(Iterable<T> values, Class<T> entityClass)
			throws DataAccessException {
		return replaceAll(values, new DocumentReplaceOptions(), entityClass);
	}

	/**
	 * Replaces the document with {@code id} with the one in the body, provided there is such a document and no
	 * precondition is violated
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
	<T> DocumentUpdateEntity<T> replace(Object id, T value, DocumentReplaceOptions options) throws DataAccessException;

	/**
	 * Replaces the document with {@code id} with the one in the body, provided there is such a document and no
	 * precondition is violated
	 *
	 * @param id
	 *            The id or key of the document
	 * @param value
	 *            A representation of a single document
	 * @return information about the document
	 * @throws DataAccessException
	 */
	default <T> DocumentUpdateEntity<T> replace(Object id, T value) throws DataAccessException {
		return replace(id, value, new DocumentReplaceOptions());
	}

	/**
	 * Retrieves the document with the given {@code id} from a collection.
	 *
	 * @param id
	 *            The id or key of the document
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @param options
	 *            Additional options, can be null
	 * @return the document identified by the id
	 * @throws DataAccessException
	 */
	<T> Optional<T> find(Object id, Class<T> entityClass, DocumentReadOptions options) throws DataAccessException;

	/**
	 * Retrieves the document with the given {@code id} from a collection.
	 *
	 * @param id
	 *            The id or key of the document
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @return the document identified by the id
	 * @throws DataAccessException
	 */
	default <T> Optional<T> find(Object id, Class<T> entityClass) throws DataAccessException {
		return find(id, entityClass, new DocumentReadOptions());
	}

	/**
	 * Retrieves all documents from a collection.
	 *
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @return the documents
	 * @throws DataAccessException
	 */
	<T> Iterable<T> findAll(Class<T> entityClass, DocumentReadOptions options) throws DataAccessException;

	default <T> Iterable<T> findAll(Class<T> entityClass) throws DataAccessException {
		return findAll(entityClass, new DocumentReadOptions());
	}

	/**
	 * Retrieves multiple documents with the given {@code ids} from a collection.
	 *
	 * @param ids
	 *            The ids or keys of the documents
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @return the documents
	 * @throws DataAccessException
	 */
	<T> Iterable<T> findAll(final Iterable<?> ids, final Class<T> entityClass, DocumentReadOptions options) throws DataAccessException;

	default <T> Iterable<T> findAll(final Iterable<?> ids, final Class<T> entityClass) throws DataAccessException {
		return findAll(ids, entityClass, new DocumentReadOptions());
	}

	/**
	 * Creates new documents from the given documents, unless there is already a document with the _key given. If no
	 * _key is given, a new unique _key is generated automatically.
	 *
	 * @param <T>
	 *
	 * @param values
	 *            A List of documents
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @param options
	 *            Additional options, can be null
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	<T> MultiDocumentEntity<DocumentCreateEntity<T>> insertAll(
		Iterable<? extends T> values,
		DocumentCreateOptions options,
		Class<T> entityClass) throws DataAccessException;

	/**
	 * Creates new documents from the given documents, unless there is already a document with the _key given. If no
	 * _key is given, a new unique _key is generated automatically.
	 *
	 * @param values
	 *            A List of documents
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @return information about the documents
	 * @throws DataAccessException
	 */
	default <T> MultiDocumentEntity<DocumentCreateEntity<T>> insertAll(Iterable<T> values, Class<T> entityClass)
			throws DataAccessException {
		return insertAll(values, new DocumentCreateOptions(), entityClass);
	}

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
	<T> DocumentCreateEntity<T> insert(T value, DocumentCreateOptions options) throws DataAccessException;

	/**
	 * Creates a new document from the given document, unless there is already a document with the _key given. If no
	 * _key is given, a new unique _key is generated automatically.
	 *
	 * @param value
	 *            A representation of a single document
	 * @return information about the document
	 */
	default <T> DocumentCreateEntity<T> insert(T value) throws DataAccessException {
		return insert(value, new DocumentCreateOptions());
	}
	/**
	 * Creates a new document from the given document, unless there is already a document with the id given. In that
	 * case it replaces the document.
	 *
	 * @param value
	 *            A representation of a single document
	 * @throws DataAccessException
	 * @since ArangoDB 3.4
	 */
	<T> void repsert(T value, AqlQueryOptions options) throws DataAccessException;

	default <T> T repsert(T value) throws DataAccessException {
		repsert(value, new AqlQueryOptions());
	}

	/**
	 * Creates new documents from the given documents, unless there already exists. In that case it replaces the
	 * documents.
	 *
	 * @param values
	 *            A List of documents
	 * @param entityClass
	 *            The entity class which represents the collection
	 * @throws DataAccessException
	 * @since ArangoDB 3.4
	 */
	<T> Iterable<T> repsertAll(Iterable<? extends T> values, Class<T> entityClass, AqlQueryOptions options) throws DataAccessException;

	default <T> Iterable<T> repsertAll(Iterable<? extends T> values, Class<T> entityClass) throws DataAccessException {
		return repsertAll(values, entityClass, new AqlQueryOptions());
	}

	/**
	 * Checks whether the document exists by reading a single document head
	 *
	 * @param id
	 *            The id or key of the document
	 * @param entityClass
	 *            The entity type representing the collection
	 * @return true if the document exists, false if not
	 * @throws DataAccessException
	 */
	boolean exists(Object id, Class<?> entityClass, DocumentExistsOptions options) throws DataAccessException;

	default boolean exists(Object id, Class<?> entityClass) throws DataAccessException {
		return exists(id, entityClass, new DocumentExistsOptions());
	}

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
	default CollectionOperations collection(String name) throws DataAccessException {
		return collection(name, new CollectionCreateOptions());
	}

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
	 * @return information about all users
	 * @throws DataAccessException
	 */
	Iterable<UserEntity> getUsers() throws DataAccessException;

	ArangoConverter getConverter();

	ResolverFactory getResolverFactory();

}
