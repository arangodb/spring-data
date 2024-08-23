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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import com.arangodb.springframework.AbstractArangoTest;

/**
 * @author Mark Vollmary
 *
 */
public abstract class AbstractTestEntityRepositoryTest<T extends IdTestEntity<ID>, ID> extends AbstractArangoTest {

	protected abstract IdTestRepository<T, ID> repository();
    protected abstract T createEntity();

	public AbstractTestEntityRepositoryTest(Class<T> clazz) {
		super(clazz);
	}

	@Test
	public void deleteAll() {
		T entity = createEntity();
		repository().save(entity);
		assertThat(repository().count(), is(1L));
		repository().deleteAll(Arrays.asList(entity));
		assertThat(repository().count(), is(0L));
	}

	@Test
	public void deleteById() {
		T entity = createEntity();
		repository().save(entity);
		assertThat(repository().count(), is(1L));
		repository().deleteById(entity.getId());
		assertThat(repository().count(), is(0L));
	}

	@Test
	public void delete() {
		T entity = createEntity();
		repository().save(entity);
		assertThat(repository().count(), is(1L));
		repository().delete(entity);
		assertThat(repository().count(), is(0L));
	}

	@Test
	public void deleteAllById() {
		T e1 = createEntity();
		T e2 = createEntity();
		repository().save(e1);
		repository().save(e2);
		assertThat(repository().count(), is(2L));
		repository().deleteAllById(Arrays.asList(e1.getId(), e2.getId()));
		assertThat(repository().count(), is(0L));
	}

	@Test
	public void existsById() {
		T entity = createEntity();
		repository().save(entity);
		assertThat(repository().existsById(entity.getId()), is(true));
	}

	@Test
	public void findAllById() {
		T entity1 = createEntity();
		T entity2 = createEntity();
		repository().saveAll(Arrays.asList(entity1, entity2));
		final Iterable<T> find = repository().findAllById(Arrays.asList(entity1.getId(), entity2.getId()));
		assertThat(StreamSupport.stream(find.spliterator(), false).count(), is(2L));
	}

	@Test
	public void findById() {
		T entity = createEntity();
		repository().save(entity);
		final Optional<T> find = repository().findById(entity.getId());
		assertThat(find.isPresent(), is(true));
	}

	@Test
	public void save() {
		repository().save(createEntity());
		assertThat(repository().count(), is(1L));
	}

	@Test
	public void saveAll() {
		repository().saveAll(Arrays.asList(createEntity(), createEntity()));
		assertThat(repository().count(), is(2L));
	}

	@Test
	public void query() {
		T entity = createEntity();
		repository().save(entity);
		final Optional<T> find = repository().findByQuery(entity.getId());
		assertThat(find.isPresent(), is(true));
		assertThat(find.get().getId(), is(entity.getId()));
	}

	@Test
	public void queryId() {
		T entity = createEntity();
		repository().save(entity);
		final Optional<ID> find = repository().findIdByQuery(entity.getId());
		assertThat(find.isPresent(), is(true));
		assertThat(find.get(), is(entity.getId()));
	}

	@Test
	public void queryEntity() {
		T entity = createEntity();
		repository().save(entity);
		final Optional<T> find = repository().findByEntity(entity);
		assertThat(find.isPresent(), is(true));
		assertThat(find.get().getId(), is(entity.getId()));
	}

}
