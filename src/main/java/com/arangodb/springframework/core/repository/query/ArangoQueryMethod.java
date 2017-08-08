package com.arangodb.springframework.core.repository.query;

import com.arangodb.springframework.annotation.Query;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;

import java.lang.reflect.Method;

/**
 * Created by F625633 on 12/07/2017.
 */
public class ArangoQueryMethod extends QueryMethod {

	
	public ArangoQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		super(method, metadata, factory);
	}

	@Override
	public Parameters getParameters() { return super.getParameters(); }

	@Override
	public Parameters createParameters(Method method) { return new ArangoParameters(method); }
}
