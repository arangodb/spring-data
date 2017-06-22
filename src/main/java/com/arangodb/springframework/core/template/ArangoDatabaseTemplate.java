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
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
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
import com.arangodb.springframework.core.ArangoCollectionOperations;
import com.arangodb.springframework.core.ArangoDatabaseOperations;
import com.arangodb.springframework.core.ArangoGraphOperations;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoDatabaseTemplate extends ArangoTemplateBase implements ArangoDatabaseOperations {

	private final ArangoDatabase db;

	protected ArangoDatabaseTemplate(final ArangoDatabase db,
		final PersistenceExceptionTranslator exceptionTranslator) {
		super(exceptionTranslator);
		this.db = db;
	}

	@Override
	public ArangoCollectionOperations collection(final String name) {
		return new ArangoCollectionTemplate(db.collection(name), exceptionTranslator);
	}

	@Override
	public ArangoGraphOperations graph(final String name) {
		return new ArangoGraphTemplate(db.graph(name), exceptionTranslator);
	}

	@Override
	public ArangoDBVersion getVersion() throws DataAccessException {
		try {
			return db.getVersion();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Collection<String> getAccessibleDatabases() throws DataAccessException {
		try {
			return db.getAccessibleDatabases();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public CollectionEntity createCollection(final String name) throws DataAccessException {
		try {
			return db.createCollection(name);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public CollectionEntity createCollection(final String name, final CollectionCreateOptions options)
			throws DataAccessException {
		try {
			return db.createCollection(name, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Collection<CollectionEntity> getCollections() throws DataAccessException {
		try {
			return db.getCollections();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Collection<CollectionEntity> getCollections(final CollectionsReadOptions options)
			throws DataAccessException {
		try {
			return db.getCollections(options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public IndexEntity getIndex(final String id) throws DataAccessException {
		try {
			return db.getIndex(id);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public String deleteIndex(final String id) throws DataAccessException {
		try {
			return db.deleteIndex(id);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Boolean drop() throws DataAccessException {
		try {
			return db.drop();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public void grantAccess(final String user) throws DataAccessException {
		try {
			db.grantAccess(user);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public void revokeAccess(final String user) throws DataAccessException {
		try {
			db.revokeAccess(user);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> ArangoCursor<T> query(
		final String query,
		final Map<String, Object> bindVars,
		final AqlQueryOptions options,
		final Class<T> type) throws DataAccessException {
		try {
			return db.query(query, bindVars, options, type);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> ArangoCursor<T> cursor(final String cursorId, final Class<T> type) throws DataAccessException {
		try {
			return db.cursor(cursorId, type);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public AqlExecutionExplainEntity explainQuery(
		final String query,
		final Map<String, Object> bindVars,
		final AqlQueryExplainOptions options) throws DataAccessException {
		try {
			return db.explainQuery(query, bindVars, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public AqlParseEntity parseQuery(final String query) throws DataAccessException {
		try {
			return db.parseQuery(query);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public void clearQueryCache() throws DataAccessException {
		try {
			db.clearQueryCache();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public QueryCachePropertiesEntity getQueryCacheProperties() throws DataAccessException {
		try {
			return db.getQueryCacheProperties();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public QueryCachePropertiesEntity setQueryCacheProperties(final QueryCachePropertiesEntity properties)
			throws DataAccessException {
		try {
			return db.setQueryCacheProperties(properties);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public QueryTrackingPropertiesEntity getQueryTrackingProperties() throws DataAccessException {
		try {
			return db.getQueryTrackingProperties();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public QueryTrackingPropertiesEntity setQueryTrackingProperties(final QueryTrackingPropertiesEntity properties)
			throws DataAccessException {
		try {
			return db.setQueryTrackingProperties(properties);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Collection<QueryEntity> getCurrentlyRunningQueries() throws DataAccessException {
		try {
			return db.getCurrentlyRunningQueries();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Collection<QueryEntity> getSlowQueries() throws DataAccessException {
		try {
			return db.getSlowQueries();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public void clearSlowQueries() throws DataAccessException {
		try {
			db.clearSlowQueries();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public void killQuery(final String id) throws DataAccessException {
		try {
			db.killQuery(id);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public void createAqlFunction(final String name, final String code, final AqlFunctionCreateOptions options)
			throws DataAccessException {
		try {
			db.createAqlFunction(name, code, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public void deleteAqlFunction(final String name, final AqlFunctionDeleteOptions options)
			throws DataAccessException {
		try {
			db.deleteAqlFunction(name, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Collection<AqlFunctionEntity> getAqlFunctions(final AqlFunctionGetOptions options)
			throws DataAccessException {
		try {
			return db.getAqlFunctions(options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public GraphEntity createGraph(final String name, final Collection<EdgeDefinition> edgeDefinitions)
			throws DataAccessException {
		try {
			return db.createGraph(name, edgeDefinitions);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public GraphEntity createGraph(
		final String name,
		final Collection<EdgeDefinition> edgeDefinitions,
		final GraphCreateOptions options) throws DataAccessException {
		try {
			return db.createGraph(name, edgeDefinitions, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Collection<GraphEntity> getGraphs() throws DataAccessException {
		try {
			return db.getGraphs();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> T transaction(final String action, final Class<T> type, final TransactionOptions options)
			throws DataAccessException {
		try {
			return db.transaction(action, type, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public DatabaseEntity getInfo() throws DataAccessException {
		try {
			return db.getInfo();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <V, E> TraversalEntity<V, E> executeTraversal(
		final Class<V> vertexClass,
		final Class<E> edgeClass,
		final TraversalOptions options) throws DataAccessException {
		try {
			return db.executeTraversal(vertexClass, edgeClass, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> T getDocument(final String id, final Class<T> type) throws DataAccessException {
		try {
			return db.getDocument(id, type);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public <T> T getDocument(final String id, final Class<T> type, final DocumentReadOptions options)
			throws DataAccessException {
		try {
			return db.getDocument(id, type, options);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public void reloadRouting() throws DataAccessException {
		try {
			db.reloadRouting();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

}
