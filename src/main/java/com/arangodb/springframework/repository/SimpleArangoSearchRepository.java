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

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.arangodb.ArangoCursor;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.util.AqlUtils;
import com.arangodb.util.MapBuilder;

/**
 * @author Mark Vollmary
 *
 */
@Repository
public class SimpleArangoSearchRepository<T> implements ArangoSearchRepository<T> {

	private final ArangoOperations arangoOperations;
	private final Class<T> domainClass;

	public SimpleArangoSearchRepository(final ArangoOperations arangoOperations, final Class<T> domainClass) {
		super();
		this.arangoOperations = arangoOperations;
		this.domainClass = domainClass;
	}

	@Override
	public Optional<T> findById(final String id) {
		return Optional.ofNullable(arangoOperations.query("FOR e IN @@view FILTER e._id == @id LIMIT 1 RETURN e",
			new MapBuilder().put("@view", domainClass).put("id", id).get(), domainClass).first());
	}

	@Override
	public boolean existsById(final String id) {
		return arangoOperations
				.query("FOR e IN @@view FILTER e._id == @id LIMIT 1 RETURN true",
					new MapBuilder().put("@view", domainClass).put("id", id).get(), Boolean.class)
				.first() == Boolean.TRUE;
	}

	@Override
	public Iterable<T> findAll() {
		return arangoOperations.query("FOR e IN @@view RETURN e", new MapBuilder().put("@view", domainClass).get(),
			domainClass);
	}

	@Override
	public Iterable<T> findAllById(final Iterable<String> ids) {
		return arangoOperations.query("FOR e IN @@view FILTER e._id IN @ids RETURN e",
			new MapBuilder().put("@view", domainClass).put("ids", ids).get(), domainClass);
	}

	@Override
	public long count() {
		return arangoOperations
				.query("RETURN COUNT(@@view)", new MapBuilder().put("@view", domainClass).get(), Long.class).first();
	}

	@Override
	public Iterable<T> findAll(final Sort sort) {
		return arangoOperations.query("FOR e IN @@view " + buildSortClause(sort, "e") + " RETURN e",
			new MapBuilder().put("@view", domainClass).get(), domainClass);
	}

	@Override
	public Page<T> findAll(final Pageable pageable) {
		final ArangoCursor<T> result = arangoOperations.query(
			"FOR e IN @@view " + buildPageableClause(pageable, "e") + " RETURN e",
			new MapBuilder().put("@view", domainClass).get(), domainClass);
		final List<T> content = result.asListRemaining();
		return new PageImpl<>(content, pageable, result.getStats().getFullCount());
	}

	@Override
	public <S extends T> Optional<S> findOne(final Example<S> example) {
		return null;
	}

	@Override
	public <S extends T> Iterable<S> findAll(final Example<S> example) {
		return null;
	}

	@Override
	public <S extends T> Iterable<S> findAll(final Example<S> example, final Sort sort) {
		return null;
	}

	@Override
	public <S extends T> Page<S> findAll(final Example<S> example, final Pageable pageable) {
		return null;
	}

	@Override
	public <S extends T> long count(final Example<S> example) {
		return 0;
	}

	@Override
	public <S extends T> boolean exists(final Example<S> example) {
		return false;
	}

	private String buildPageableClause(final Pageable pageable, final String varName) {
		return pageable == null ? "" : AqlUtils.buildPageableClause(pageable, varName);
	}

	private String buildSortClause(final Sort sort, final String varName) {
		return sort == null ? "" : AqlUtils.buildSortClause(sort, varName);
	}
}
