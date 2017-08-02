package com.arangodb.springframework.core.repository.query;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by F625633 on 12/07/2017.
 */
public class ArangoParameters extends Parameters<ArangoParameters, ArangoParameters.ArangoParameter> {

	public ArangoParameters(Method method) { super(method); }

	public ArangoParameters(List<ArangoParameter> parameters) { super(parameters); }

	@Override
	protected ArangoParameter createParameter(MethodParameter parameter) { return new ArangoParameter(parameter); }

	@Override
	protected ArangoParameters createFrom(List<ArangoParameter> parameters) {
		return new ArangoParameters(parameters);
	}

	protected static class ArangoParameter extends Parameter {
		public ArangoParameter(MethodParameter parameter) { super(parameter); }
	}
}
