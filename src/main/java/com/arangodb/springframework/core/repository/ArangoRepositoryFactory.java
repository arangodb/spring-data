package com.arangodb.springframework.core.repository;

import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import com.arangodb.springframework.core.repository.query.ArangoQueryLookupStrategy;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;

import java.io.ObjectInput;
import java.io.Serializable;

/**
 * Created by F625633 on 06/07/2017.
 */
public class ArangoRepositoryFactory extends RepositoryFactorySupport {

	private ArangoOperations arangoOperations;

	public ArangoRepositoryFactory(ArangoOperations arangoOperations) {
		this.arangoOperations = arangoOperations;
	}

	@Override public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return null;
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation metadata) {
		return new SimpleArangoRepository(arangoOperations, metadata.getDomainType());
	}

	@Override protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleArangoRepository.class;
	}

	@Override
	protected QueryLookupStrategy getQueryLookupStrategy(QueryLookupStrategy.Key key, EvaluationContextProvider evaluationContextProvider) {
		return new ArangoQueryLookupStrategy(arangoOperations);
	}

}
