/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.repository.query.ArangoParameters.ArangoParameter;

/**
 *
 * @author Audrius Malele
 * @author Mark McCormick
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public class StringBasedArangoQuery extends AbstractArangoQuery {

	private static final Pattern BIND_PARAM_PATTERN = Pattern.compile("@(@?[A-Za-z0-9][A-Za-z0-9_]*)");

	private final String query;
	private final Set<String> queryBindParams;

	public StringBasedArangoQuery(ArangoQueryMethod method, ArangoOperations operations) {
		this(method.getAnnotatedQuery(), method, operations);
	}

	public StringBasedArangoQuery(String query, ArangoQueryMethod method, ArangoOperations operations) {
		super(method, operations);
		Assert.notNull(query, "Query must not be null!");

		this.query = query;
		this.queryBindParams = getBindParamsInQuery();
	}

	@Override
	protected String createQuery(final ArangoParameterAccessor accessor, final Map<String, Object> bindVars,
			final AqlQueryOptions options) {

		final Map<String, Object> bindVarsInParams = accessor.getBindVars();
		if (bindVarsInParams != null) {
			bindVars.putAll(bindVarsInParams);
		}

		final ArangoParameters bindableParams = accessor.getParameters().getBindableParameters();
		final int bindableParamsSize = bindableParams.getNumberOfParameters();

		for (int i = 0; i < bindableParamsSize; ++i) {
			final ArangoParameter param = bindableParams.getParameter(i);
			final Object value = accessor.getBindableValue(i);
			if (param.isNamedParameter()) {
				bindVars.put(param.getName(), value);
			} else {
				final String key = String.valueOf(param.getIndex());
				final String collectionKey = "@" + key;
				if (queryBindParams.contains(collectionKey)) {
					bindVars.put(collectionKey, value);
				} else {
					bindVars.put(key, value);
				}
			}
		}

		return query;
	}

	@Override
	protected boolean isCountQuery() {
		return false;
	}

	@Override
	protected boolean isExistsQuery() {
		return false;
	}

	private Set<String> getBindParamsInQuery() {
		final String fixedQuery = removeAqlStringLiterals(query);
		final Set<String> bindings = new HashSet<>();
		final Matcher matcher = BIND_PARAM_PATTERN.matcher(fixedQuery);
		while (matcher.find()) {
			bindings.add(matcher.group(1));
		}
		return bindings;
	}

	private String removeAqlStringLiterals(final String query) {
		final StringBuilder fixedQuery = new StringBuilder();
		for (int i = 0; i < query.length(); ++i) {
			if (query.charAt(i) == '"') {
				for (++i; i < query.length(); ++i) {
					if (query.charAt(i) == '"') {
						++i;
						break;
					}
					if (query.charAt(i) == '\\') {
						++i;
					}
				}
			} else if (query.charAt(i) == '\'') {
				for (++i; i < query.length(); ++i) {
					if (query.charAt(i) == '\'') {
						++i;
						break;
					}
					if (query.charAt(i) == '\\') {
						++i;
					}
				}
			}
			fixedQuery.append(query.charAt(i));
		}
		return fixedQuery.toString();
	}

}
