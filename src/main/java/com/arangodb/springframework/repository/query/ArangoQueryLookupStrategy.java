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

package com.arangodb.springframework.repository.query;

import java.lang.reflect.Method;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;

import com.arangodb.springframework.core.ArangoOperations;

/**
 * Created by F625633 on 12/07/2017.
 */
public class ArangoQueryLookupStrategy implements QueryLookupStrategy {

	private final ArangoOperations operations;

	public ArangoQueryLookupStrategy(final ArangoOperations operations) {
		this.operations = operations;
	}

	@Override
	public RepositoryQuery resolveQuery(
		final Method method,
		final RepositoryMetadata metadata,
		final ProjectionFactory factory,
		final NamedQueries namedQueries) {
		return new ArangoAqlQuery(metadata.getDomainType(), method, metadata, operations, factory);
	}
}
