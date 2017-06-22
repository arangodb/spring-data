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

import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.GraphEntity;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public interface ArangoGraphOperations {

	ArangoVertexCollectionOperations vertexCollection(final String name);

	ArangoEdgeCollectionOperations edgeCollection(final String name);

	GraphEntity removeEdgeDefinition(final String definitionName) throws DataAccessException;

	GraphEntity replaceEdgeDefinition(final EdgeDefinition definition) throws DataAccessException;

	GraphEntity addEdgeDefinition(final EdgeDefinition definition) throws DataAccessException;

	Collection<String> getEdgeDefinitions() throws DataAccessException;

	GraphEntity addVertexCollection(final String name) throws DataAccessException;

	Collection<String> getVertexCollections() throws DataAccessException;

	GraphEntity getInfo() throws DataAccessException;

	void drop() throws DataAccessException;

}
