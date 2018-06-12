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

package com.arangodb.springframework.repository;

import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AnnotationRepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import com.arangodb.ArangoCursor;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import com.arangodb.springframework.repository.query.ArangoQueryMethod;
import com.arangodb.springframework.repository.query.DerivedArangoQuery;
import com.arangodb.springframework.repository.query.StringBasedArangoQuery;

/**
 * 
 * @author Audrius Malele
 * @author Mark McCormick
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public class ArangoRepositoryFactory extends RepositoryFactorySupport {

	private final ArangoOperations arangoOperations;
	private final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context;

	public ArangoRepositoryFactory(final ArangoOperations arangoOperations) {
		this.arangoOperations = arangoOperations;
		this.context = arangoOperations.getConverter().getMappingContext();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, ID> ArangoEntityInformation<T, ID> getEntityInformation(final Class<T> domainClass) {
		return new ArangoPersistentEntityInformation<T, ID>(
				(ArangoPersistentEntity<T>) context.getRequiredPersistentEntity(domainClass));
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
	protected RepositoryMetadata getRepositoryMetadata(Class<?> repositoryInterface) {
		Assert.notNull(repositoryInterface, "Repository interface must not be null!");

		return Repository.class.isAssignableFrom(repositoryInterface)
				? new DefaultArangoRepositoryMetadata(repositoryInterface)
				: new AnnotationArangoRepositoryMetadata(repositoryInterface);
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(
		final QueryLookupStrategy.Key key,
		final EvaluationContextProvider evaluationContextProvider) {

		QueryLookupStrategy strategy = null;
		switch (key) {
		case CREATE_IF_NOT_FOUND:
			strategy = new DefaultArangoQueryLookupStrategy(arangoOperations);
			break;
		case CREATE:
			break;
		case USE_DECLARED_QUERY:
			break;
		}
		return Optional.ofNullable(strategy);
	}

	static class DefaultArangoQueryLookupStrategy implements QueryLookupStrategy {

		private final ArangoOperations operations;

		public DefaultArangoQueryLookupStrategy(final ArangoOperations operations) {
			this.operations = operations;
		}

		@Override
		public RepositoryQuery resolveQuery(
			final Method method,
			final RepositoryMetadata metadata,
			final ProjectionFactory factory,
			final NamedQueries namedQueries) {

			final ArangoQueryMethod queryMethod = new ArangoQueryMethod(method, metadata, factory);
			final String namedQueryName = queryMethod.getNamedQueryName();

			if (namedQueries.hasQuery(namedQueryName)) {
				final String namedQuery = namedQueries.getQuery(namedQueryName);
				return new StringBasedArangoQuery(namedQuery, queryMethod, operations);
			} else if (queryMethod.hasAnnotatedQuery()) {
				return new StringBasedArangoQuery(queryMethod, operations);
			} else {
				return new DerivedArangoQuery(queryMethod, operations);
			}
		}

	}

	static class DefaultArangoRepositoryMetadata extends DefaultRepositoryMetadata {

		private final TypeInformation<?> typeInformation;

		public DefaultArangoRepositoryMetadata(Class<?> repositoryInterface) {
			super(repositoryInterface);
			typeInformation = ClassTypeInformation.from(repositoryInterface);
		}

		@Override
		public Class<?> getReturnedDomainClass(Method method) {
			if (ArangoCursor.class.isAssignableFrom(method.getReturnType())) {
				return typeInformation.getReturnType(method).getRequiredComponentType().getType();
			} else {
				return super.getReturnedDomainClass(method);
			}
		}

	}

	static class AnnotationArangoRepositoryMetadata extends AnnotationRepositoryMetadata {

		private final TypeInformation<?> typeInformation;

		public AnnotationArangoRepositoryMetadata(Class<?> repositoryInterface) {
			super(repositoryInterface);
			typeInformation = ClassTypeInformation.from(repositoryInterface);
		}

		@Override
		public Class<?> getReturnedDomainClass(Method method) {
			if (ArangoCursor.class.isAssignableFrom(method.getReturnType())) {
				return typeInformation.getReturnType(method).getRequiredComponentType().getType();
			} else {
				return super.getReturnedDomainClass(method);
			}
		}

	}

}
