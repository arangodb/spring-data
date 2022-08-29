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

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.util.AqlUtils;
import com.arangodb.springframework.repository.query.ArangoParameters.ArangoParameter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Audrius Malele
 * @author Mark McCormick
 * @author Mark Vollmary
 * @author Christian Lechner
 * @author Michele Rastelli
 */
public class StringBasedArangoQuery extends AbstractArangoQuery {
	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private static final String PAGEABLE_PLACEHOLDER = "#pageable";
	private static final Pattern PAGEABLE_PLACEHOLDER_PATTERN = Pattern.compile(Pattern.quote(PAGEABLE_PLACEHOLDER));

	private static final String SORT_PLACEHOLDER = "#sort";
	private static final Pattern SORT_PLACEHOLDER_PATTERN = Pattern.compile(Pattern.quote(SORT_PLACEHOLDER));

	private static final String COLLECTION_PLACEHOLDER = "#collection";
	private static final Pattern COLLECTION_PLACEHOLDER_PATTERN = Pattern
			.compile(Pattern.quote(COLLECTION_PLACEHOLDER));

	private static final Pattern BIND_PARAM_PATTERN = Pattern.compile("@(@?[A-Za-z0-9][A-Za-z0-9_]*)");

	private final String query;
	private	final String collectionName;
	private final Expression queryExpression;
	private final Set<String> queryBindParams;
	private final ApplicationContext applicationContext;

	public StringBasedArangoQuery(final ArangoQueryMethod method, final ArangoOperations operations,
								  final QueryTransactionBridge transactionBridge, final ApplicationContext applicationContext) {
		this(method.getAnnotatedQuery(), method, operations, transactionBridge, applicationContext);
	}

	public StringBasedArangoQuery(final String query, final ArangoQueryMethod method,
		final ArangoOperations operations, final QueryTransactionBridge transactionBridge,
		final ApplicationContext applicationContext) {
		super(method, operations, transactionBridge);
		Assert.notNull(query, "Query must not be null!");

		this.query = query;
		collectionName = AqlUtils.buildCollectionName(operations.collection(domainClass).name());
		this.applicationContext = applicationContext;

		assertSinglePageablePlaceholder();
		assertSingleSortPlaceholder();

		this.queryBindParams = getBindParamsInQuery();
		queryExpression = PARSER.parseExpression(query, ParserContext.TEMPLATE_EXPRESSION);
	}

	@Override
	protected QueryWithCollections createQuery(
            final ArangoParameterAccessor accessor,
            final Map<String, Object> bindVars) {

		extractBindVars(accessor, bindVars);

		return new QueryWithCollections(prepareQuery(accessor), allCollectionNames(bindVars));
	}

	private Collection<String> allCollectionNames(Map<String, Object> bindVars) {
		HashSet<String> allCollections = new HashSet<>();
		if (!Modifier.isAbstract(domainClass.getModifiers())) {
			allCollections.add(collectionName);
		}
		bindVars.entrySet().stream()
				.filter(entry -> entry.getKey().startsWith("@"))
				.map(Map.Entry::getValue)
				.map(value -> value instanceof Class ? getCollectionName((Class<?>) value): value.toString())
				.filter(Objects::nonNull)
				.forEach(allCollections::add);
		return allCollections;
	}

	private String getCollectionName(Class<?> value) {
		Document document = AnnotationUtils.findAnnotation(value, Document.class);
		if (document != null) {
			return document.value();
		}
		Edge edge = AnnotationUtils.findAnnotation(value, Edge.class);
		if (edge != null) {
			return edge.value();
		}
		return null;
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
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setRootObject(applicationContext);
		context.setBeanResolver(new BeanFactoryResolver(applicationContext));
		context.addPropertyAccessor(new BeanFactoryAccessor());
		context.setVariable("collection", collectionName);
		context.setVariables(accessor.getSpelVars());

		String preparedQuery = queryExpression.getValue(context, String.class);

		final Matcher collectionMatcher = COLLECTION_PLACEHOLDER_PATTERN.matcher(preparedQuery);
		if (collectionMatcher.find()) {
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
