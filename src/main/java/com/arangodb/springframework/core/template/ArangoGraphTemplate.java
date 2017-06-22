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

import com.arangodb.ArangoGraph;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.GraphEntity;
import com.arangodb.springframework.core.ArangoEdgeCollectionOperations;
import com.arangodb.springframework.core.ArangoGraphOperations;
import com.arangodb.springframework.core.ArangoVertexCollectionOperations;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoGraphTemplate extends ArangoTemplateBase implements ArangoGraphOperations {

	private final ArangoGraph graph;

	protected ArangoGraphTemplate(final ArangoGraph graph, final PersistenceExceptionTranslator exceptionTranslator) {
		super(exceptionTranslator);
		this.graph = graph;
	}

	@Override
	public ArangoVertexCollectionOperations vertexCollection(final String name) {
		return new ArangoVertexCollectionTemplate(graph.vertexCollection(name), exceptionTranslator);
	}

	@Override
	public ArangoEdgeCollectionOperations edgeCollection(final String name) {
		return new ArangoEdgeCollectionTemplate(graph.edgeCollection(name), exceptionTranslator);
	}

	@Override
	public void drop() throws DataAccessException {
		try {
			graph.drop();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public GraphEntity getInfo() throws DataAccessException {
		try {
			return graph.getInfo();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Collection<String> getVertexCollections() throws DataAccessException {
		try {
			return graph.getVertexCollections();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public GraphEntity addVertexCollection(final String name) throws DataAccessException {
		try {
			return graph.addVertexCollection(name);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public Collection<String> getEdgeDefinitions() throws DataAccessException {
		try {
			return graph.getEdgeDefinitions();
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public GraphEntity addEdgeDefinition(final EdgeDefinition definition) throws DataAccessException {
		try {
			return graph.addEdgeDefinition(definition);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public GraphEntity replaceEdgeDefinition(final EdgeDefinition definition) throws DataAccessException {
		try {
			return graph.replaceEdgeDefinition(definition);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

	@Override
	public GraphEntity removeEdgeDefinition(final String definitionName) throws DataAccessException {
		try {
			return graph.removeEdgeDefinition(definitionName);
		} catch (final RuntimeException e) {
			throw translateExceptionIfPossible(e);
		}
	}

}
