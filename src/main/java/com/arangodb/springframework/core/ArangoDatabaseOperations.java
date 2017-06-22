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
import com.arangodb.entity.AqlExecutionExplainEntity;
import com.arangodb.entity.AqlFunctionEntity;
import com.arangodb.entity.AqlParseEntity;
import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.DatabaseEntity;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.GraphEntity;
import com.arangodb.entity.IndexEntity;
import com.arangodb.entity.QueryCachePropertiesEntity;
import com.arangodb.entity.QueryEntity;
import com.arangodb.entity.QueryTrackingPropertiesEntity;
import com.arangodb.entity.TraversalEntity;
import com.arangodb.model.AqlFunctionCreateOptions;
import com.arangodb.model.AqlFunctionDeleteOptions;
import com.arangodb.model.AqlFunctionGetOptions;
import com.arangodb.model.AqlQueryExplainOptions;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.CollectionsReadOptions;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.GraphCreateOptions;
import com.arangodb.model.TransactionOptions;
import com.arangodb.model.TraversalOptions;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public interface ArangoDatabaseOperations {

	ArangoCollectionOperations collection(final String name);

	ArangoGraphOperations graph(final String name);

	void reloadRouting() throws DataAccessException;

	<T> T getDocument(final String id, final Class<T> type, final DocumentReadOptions options)
			throws DataAccessException;

	<T> T getDocument(final String id, final Class<T> type) throws DataAccessException;

	<V, E> TraversalEntity<V, E> executeTraversal(
		final Class<V> vertexClass,
		final Class<E> edgeClass,
		final TraversalOptions options) throws DataAccessException;

	DatabaseEntity getInfo() throws DataAccessException;

	<T> T transaction(final String action, final Class<T> type, final TransactionOptions options)
			throws DataAccessException;

	Collection<GraphEntity> getGraphs() throws DataAccessException;

	GraphEntity createGraph(
		final String name,
		final Collection<EdgeDefinition> edgeDefinitions,
		final GraphCreateOptions options) throws DataAccessException;

	GraphEntity createGraph(final String name, final Collection<EdgeDefinition> edgeDefinitions)
			throws DataAccessException;

	Collection<AqlFunctionEntity> getAqlFunctions(final AqlFunctionGetOptions options) throws DataAccessException;

	void deleteAqlFunction(final String name, final AqlFunctionDeleteOptions options) throws DataAccessException;

	void createAqlFunction(final String name, final String code, final AqlFunctionCreateOptions options)
			throws DataAccessException;

	void killQuery(final String id) throws DataAccessException;

	void clearSlowQueries() throws DataAccessException;

	Collection<QueryEntity> getSlowQueries() throws DataAccessException;

	Collection<QueryEntity> getCurrentlyRunningQueries() throws DataAccessException;

	QueryTrackingPropertiesEntity setQueryTrackingProperties(final QueryTrackingPropertiesEntity properties)
			throws DataAccessException;

	QueryTrackingPropertiesEntity getQueryTrackingProperties() throws DataAccessException;

	QueryCachePropertiesEntity setQueryCacheProperties(final QueryCachePropertiesEntity properties)
			throws DataAccessException;

	QueryCachePropertiesEntity getQueryCacheProperties() throws DataAccessException;

	void clearQueryCache() throws DataAccessException;

	AqlParseEntity parseQuery(final String query) throws DataAccessException;

	AqlExecutionExplainEntity explainQuery(
		final String query,
		final Map<String, Object> bindVars,
		final AqlQueryExplainOptions options) throws DataAccessException;

	<T> ArangoCursor<T> cursor(final String cursorId, final Class<T> type) throws DataAccessException;

	<T> ArangoCursor<T> query(
		final String query,
		final Map<String, Object> bindVars,
		final AqlQueryOptions options,
		final Class<T> type) throws DataAccessException;

	void revokeAccess(final String user) throws DataAccessException;

	void grantAccess(final String user) throws DataAccessException;

	Boolean drop() throws DataAccessException;

	String deleteIndex(final String id) throws DataAccessException;

	IndexEntity getIndex(final String id) throws DataAccessException;

	Collection<CollectionEntity> getCollections(final CollectionsReadOptions options) throws DataAccessException;

	Collection<CollectionEntity> getCollections() throws DataAccessException;

	CollectionEntity createCollection(final String name, final CollectionCreateOptions options)
			throws DataAccessException;

	CollectionEntity createCollection(final String name) throws DataAccessException;

	Collection<String> getAccessibleDatabases() throws DataAccessException;

	ArangoDBVersion getVersion() throws DataAccessException;

}
