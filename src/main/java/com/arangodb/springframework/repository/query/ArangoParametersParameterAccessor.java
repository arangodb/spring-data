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

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.repository.query.ParametersParameterAccessor;

import com.arangodb.model.AqlQueryOptions;

/**
 * This class provides access to parameters of a user-defined method. It wraps ParametersParameterAccessor which catches
 * special parameters Sort and Pageable, and catches Arango-specific parameters e.g. AqlQueryOptions.
 *
 * @author Audrius Malele
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public class ArangoParametersParameterAccessor extends ParametersParameterAccessor implements ArangoParameterAccessor {

    private final ArangoParameters parameters;

    public ArangoParametersParameterAccessor(ArangoQueryMethod method, Object[] values) {
        super(method.getParameters(), values);
        this.parameters = method.getParameters();
    }

    @Override
    public ArangoParameters getParameters() {
        return parameters;
    }

    @Override
    public AqlQueryOptions getQueryOptions() {
        final int optionsIndex = parameters.getQueryOptionsIndex();
        return optionsIndex == -1 ? null : getValue(optionsIndex);
    }

    @Override
    public Map<String, Object> getBindVars() {
        final int bindVarsIndex = parameters.getBindVarsIndex();
        return bindVarsIndex == -1 ? null : getValue(bindVarsIndex);
    }

    @Override
    public Map<String, Object> getSpelVars() {
        return parameters.get()
                .filter(ArangoParameters.ArangoParameter::isSpelParam)
                .collect(Collectors.toMap(it -> it.getName().get(), it -> getValue(it.getIndex())));
    }

}
