package com.arangodb.springframework.core.repository.query;

import com.arangodb.springframework.core.ArangoOperations;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;

import java.lang.reflect.Method;

/**
 * Created by F625633 on 12/07/2017.
 */
public class ArangoQueryLookupStrategy implements QueryLookupStrategy {

	private ArangoOperations operations;

	public ArangoQueryLookupStrategy(ArangoOperations operations) { this.operations = operations; }

	@Override
	public RepositoryQuery resolveQuery(
		Method method,
		RepositoryMetadata metadata,
		ProjectionFactory factory,
		NamedQueries namedQueries) {
		return new ArangoAqlQuery(metadata.getDomainType(), method, metadata, operations, factory);
	}
}
