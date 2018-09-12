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

package com.arangodb.springframework.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import com.arangodb.ArangoCursor;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.util.AqlUtils;
import com.arangodb.util.MapBuilder;

/**
 * @author Mark Vollmary
 *
 */
@Repository
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SimpleArangoSearchRepository<T> implements ArangoSearchRepository<T> {

	private final ArangoExampleConverter exampleConverter;
	private final ArangoOperations arangoOperations;
	private final Class<T> domainClass;

	public SimpleArangoSearchRepository(final ArangoOperations arangoOperations, final Class<T> domainClass) {
		super();
		this.arangoOperations = arangoOperations;
		this.domainClass = domainClass;
		this.exampleConverter = new ArangoExampleConverter(
				(ArangoMappingContext) arangoOperations.getConverter().getMappingContext());
	}

	@Override
	public Optional<T> findById(final String id) {
		return Optional.ofNullable(arangoOperations.query("FOR e IN @@view FILTER e._id == @id LIMIT 1 RETURN e",
			new MapBuilder().put("@view", getViewName()).put("id", id).get(), domainClass).first());
	}

	@Override
	public boolean existsById(final String id) {
		return arangoOperations
				.query("FOR e IN @@view FILTER e._id == @id LIMIT 1 RETURN true",
					new MapBuilder().put("@view", getViewName()).put("id", id).get(), Boolean.class)
				.first() == Boolean.TRUE;
	}

	@Override
	public Iterable<T> findAll() {
		return arangoOperations.query("FOR e IN @@view RETURN e", new MapBuilder().put("@view", getViewName()).get(),
			domainClass);
	}

	@Override
	public Iterable<T> findAllById(final Iterable<String> ids) {
		return arangoOperations.query("FOR e IN @@view FILTER e._id IN @ids RETURN e",
			new MapBuilder().put("@view", getViewName()).put("ids", ids).get(), domainClass);
	}

	@Override
	public long count() {
		final Map<String, Object> bindVars = new MapBuilder().put("@view", getViewName()).get();
		final String query = "FOR e IN @@view COLLECT WITH COUNT INTO length RETURN length";
		return arangoOperations.query(query, bindVars, Long.class).first();
	}

	@Override
	public Iterable<T> findAll(final Sort sort) {
		return _findAll(sort, null);
	}

	@Override
	public Page<T> findAll(final Pageable pageable) {
		final ArangoCursor cursor = _findAll(pageable, null);
		return new PageImpl<>(cursor.asListRemaining(), pageable, cursor.getStats().getFullCount());
	}

	@Override
	public <S extends T> Optional<S> findOne(final Example<S> example) {
		final Map<String, Object> bindVars = new MapBuilder().put("@view", getViewName()).get();
		final String query = "FOR e IN @@view " + buildSearchClause(example, bindVars) + " LIMIT 1 RETURN e";
		return Optional.ofNullable((S) arangoOperations.query(query, bindVars, domainClass).first());
	}

	@Override
	public <S extends T> Iterable<S> findAll(final Example<S> example) {
		return _findAll((Pageable) null, example);
	}

	@Override
	public <S extends T> Iterable<S> findAll(final Example<S> example, final Sort sort) {
		return _findAll(sort, example);
	}

	@Override
	public <S extends T> Page<S> findAll(final Example<S> example, final Pageable pageable) {
		final ArangoCursor cursor = _findAll(pageable, example);
		return new PageImpl<>(cursor.asListRemaining(), pageable, cursor.getStats().getFullCount());
	}

	@Override
	public <S extends T> long count(final Example<S> example) {
		final Map<String, Object> bindVars = new MapBuilder().put("@view", getViewName()).get();
		final String query = "FOR e IN @@view " + buildSearchClause(example, bindVars)
				+ " COLLECT WITH COUNT INTO length RETURN length";
		return arangoOperations.query(query, bindVars, Long.class).first();
	}

	@Override
	public <S extends T> boolean exists(final Example<S> example) {
		final Map<String, Object> bindVars = new MapBuilder().put("@view", getViewName()).get();
		final String query = "FOR e IN @@view " + buildSearchClause(example, bindVars) + " LIMIT 1 RETURN true";
		return arangoOperations.query(query, bindVars, Boolean.class).first() == Boolean.TRUE;
	}

	private <S extends T> ArangoCursor<S> _findAll(final Sort sort, @Nullable final Example<S> example) {
		final Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("@view", getViewName());

		final String query = "FOR e IN @@view " + buildSearchClause(example, bindVars) + " "
				+ buildSortClause(sort, "e") + " RETURN e";
		final ArangoCursor cursor = arangoOperations.query(query, bindVars, domainClass);
		return cursor;
	}

	private <S extends T> ArangoCursor<S> _findAll(final Pageable pageable, @Nullable final Example<S> example) {
		final Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("@view", getViewName());

		final String query = "FOR e IN @@view " + buildSearchClause(example, bindVars) + " "
				+ buildPageableClause(pageable, "e") + " RETURN e";
		final AqlQueryOptions options = new AqlQueryOptions();
		if (pageable != null && pageable.isPaged()) {
			options.fullCount(true);
		}
		final ArangoCursor cursor = arangoOperations.query(query, bindVars, options, domainClass);
		return cursor;
	}

	private <S extends T> String buildSearchClause(final Example<S> example, final Map<String, Object> bindVars) {
		if (example == null) {
			return "";
		}

		final String predicate = exampleConverter.convertExampleToPredicate(example, bindVars);
		return predicate == null ? "" : "FILTER " + predicate;
	}

	private String buildPageableClause(final Pageable pageable, final String varName) {
		return pageable == null ? "" : AqlUtils.buildPageableClause(pageable, varName);
	}

	private String buildSortClause(final Sort sort, final String varName) {
		return sort == null ? "" : AqlUtils.buildSortClause(sort, varName);
	}

	private String getViewName() {
		return arangoOperations.getConverter().getMappingContext().getPersistentEntity(domainClass).getArangoSearch()
				.get();
	}
}
