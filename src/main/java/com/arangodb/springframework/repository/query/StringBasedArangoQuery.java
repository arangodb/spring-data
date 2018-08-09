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
import com.arangodb.springframework.core.util.AqlUtils;
import com.arangodb.springframework.repository.query.ArangoParameters.ArangoParameter;

/**
 * 
 * @author Audrius Malele
 * @author Mark McCormick
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public class StringBasedArangoQuery extends AbstractArangoQuery {

	private static final String PAGEABLE_PLACEHOLDER = "#pageable";
	private static final Pattern PAGEABLE_PLACEHOLDER_PATTERN = Pattern.compile(Pattern.quote(PAGEABLE_PLACEHOLDER));

	private static final String SORT_PLACEHOLDER = "#sort";
	private static final Pattern SORT_PLACEHOLDER_PATTERN = Pattern.compile(Pattern.quote(SORT_PLACEHOLDER));

	private static final String COLLECTION_PLACEHOLDER = "#collection";
	private static final Pattern COLLECTION_PLACEHOLDER_PATTERN = Pattern
			.compile(Pattern.quote(COLLECTION_PLACEHOLDER));

	private static final Pattern BIND_PARAM_PATTERN = Pattern.compile("@(@?[A-Za-z0-9][A-Za-z0-9_]*)");

	private final String query;
	private final Set<String> queryBindParams;

	public StringBasedArangoQuery(final ArangoQueryMethod method, final ArangoOperations operations) {
		this(method.getAnnotatedQuery(), method, operations);
	}

	public StringBasedArangoQuery(final String query, final ArangoQueryMethod method,
		final ArangoOperations operations) {
		super(method, operations);
		Assert.notNull(query, "Query must not be null!");

		this.query = query;

		assertSinglePageablePlaceholder();
		assertSingleSortPlaceholder();

		this.queryBindParams = getBindParamsInQuery();
	}

	@Override
	protected String createQuery(
		final ArangoParameterAccessor accessor,
		final Map<String, Object> bindVars,
		final AqlQueryOptions options) {

		extractBindVars(accessor, bindVars);

		return prepareQuery(accessor);
	}

	@Override
	protected boolean isCountQuery() {
		return false;
	}

	@Override
	protected boolean isExistsQuery() {
		return false;
	}

	private String prepareQuery(final ArangoParameterAccessor accessor) {
		String preparedQuery = query;

		final Matcher collectionMatcher = COLLECTION_PLACEHOLDER_PATTERN.matcher(preparedQuery);
		if (collectionMatcher.find()) {
			final String collectionName = AqlUtils.buildCollectionName(operations.collection(domainClass).name());
			preparedQuery = collectionMatcher.replaceAll(collectionName);
		}

		if (accessor.getParameters().hasPageableParameter()) {
			final String pageableClause = AqlUtils.buildPageableClause(accessor.getPageable());
			preparedQuery = PAGEABLE_PLACEHOLDER_PATTERN.matcher(preparedQuery).replaceFirst(pageableClause);
		} else if (accessor.getParameters().hasSortParameter()) {
			final String sortClause = AqlUtils.buildSortClause(accessor.getSort());
			preparedQuery = SORT_PLACEHOLDER_PATTERN.matcher(preparedQuery).replaceFirst(sortClause);
		}

		return preparedQuery;
	}

	private void extractBindVars(final ArangoParameterAccessor accessor, final Map<String, Object> bindVars) {
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
				bindVars.put(param.getName().get(), value);
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

	private void assertSinglePageablePlaceholder() {
		if (method.getParameters().hasPageableParameter()) {
			final int firstOccurrence = query.indexOf(PAGEABLE_PLACEHOLDER);
			final int secondOccurrence = query.indexOf(PAGEABLE_PLACEHOLDER,
				firstOccurrence + PAGEABLE_PLACEHOLDER.length());

			Assert.isTrue(firstOccurrence > -1 && secondOccurrence < 0,
				String.format(
					"Native query with Pageable param must contain exactly one pageable placeholder (%s)! Offending method: %s",
					PAGEABLE_PLACEHOLDER, method));
		}
	}

	private void assertSingleSortPlaceholder() {
		if (method.getParameters().hasSortParameter()) {
			final int firstOccurrence = query.indexOf(SORT_PLACEHOLDER);
			final int secondOccurrence = query.indexOf(SORT_PLACEHOLDER, firstOccurrence + SORT_PLACEHOLDER.length());

			Assert.isTrue(firstOccurrence > -1 && secondOccurrence < 0,
				String.format(
					"Native query with Sort param must contain exactly one sort placeholder (%s)! Offending method: %s",
					SORT_PLACEHOLDER, method));
		}
	}

}
