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

package com.arangodb.springframework.core.repository.query;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;

/**
 * Created by F625633 on 12/07/2017.
 */
public class ArangoParameters extends Parameters<ArangoParameters, ArangoParameters.ArangoParameter> {

	public ArangoParameters(final Method method) {
		super(method);
	}

	public ArangoParameters(final List<ArangoParameter> parameters) {
		super(parameters);
	}

	@Override
	protected ArangoParameter createParameter(final MethodParameter parameter) {
		return new ArangoParameter(parameter);
	}

	@Override
	protected ArangoParameters createFrom(final List<ArangoParameter> parameters) {
		return new ArangoParameters(parameters);
	}

	protected static class ArangoParameter extends Parameter {
		public ArangoParameter(final MethodParameter parameter) {
			super(parameter);
		}
	}
}
