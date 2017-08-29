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

package com.arangodb.springframework.core.repository;

import java.io.Serializable;

import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;

import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.repository.query.ArangoQueryLookupStrategy;

/**
 * Created by F625633 on 06/07/2017.
 */
public class ArangoRepositoryFactory extends RepositoryFactorySupport {

	private final ArangoOperations arangoOperations;

	public ArangoRepositoryFactory(final ArangoOperations arangoOperations) {
		this.arangoOperations = arangoOperations;
	}

	@Override
	public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(final Class<T> domainClass) {
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected Object getTargetRepository(final RepositoryInformation metadata) {
		return new SimpleArangoRepository(arangoOperations, metadata.getDomainType());
	}

	@Override
	protected Class<?> getRepositoryBaseClass(final RepositoryMetadata metadata) {
		return SimpleArangoRepository.class;
	}

	@Override
	protected QueryLookupStrategy getQueryLookupStrategy(
		final QueryLookupStrategy.Key key,
		final EvaluationContextProvider evaluationContextProvider) {
		return new ArangoQueryLookupStrategy(arangoOperations);
	}

}
