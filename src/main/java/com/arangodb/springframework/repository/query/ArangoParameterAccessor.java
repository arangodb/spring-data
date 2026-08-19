/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
 * Copyright 2023 Hewlett Packard Enterprise Development LP.
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
 * Copyright holder is Hewlett Packard Enterprise Development LP.
 */

package com.arangodb.springframework.repository.query;

import java.util.Map;

import org.springframework.data.repository.query.ParameterAccessor;

import com.arangodb.model.AqlQueryOptions;

import com.arangodb.springframework.repository.query.filter.Filterable;

import com.arangodb.springframework.repository.query.search.Searchable;

/**
 * 
 * @author Christian Lechner
 */
public interface ArangoParameterAccessor extends ParameterAccessor {

	ArangoParameters getParameters();
	
	AqlQueryOptions getQueryOptions();

	Map<String, Object> getBindVars();

	Map<String, Object> getSpelVars();
	
	Filterable getFilter(String placeholder);

	Searchable getSearch();
}
