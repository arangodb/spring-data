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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.testdata.IdTestEntity;

/**
 * @author Mark Vollmary
 * @param <ID>
 *
 */
public abstract class AbstractTestEntityRepositoryTest<ID> extends AbstractArangoTest {

	@Autowired
	protected IdTestRepository<ID> repository;

	public AbstractTestEntityRepositoryTest(final Class<?>... collections) {
		super(IdTestEntity.class);
	}

	@Test
	public void deleteAll() {
		final IdTestEntity<ID> entity = new IdTestEntity<>();
		repository.save(entity);
		assertThat(repository.count(), is(1L));
		repository.deleteAll(Arrays.asList(entity));
		assertThat(repository.count(), is(0L));
	}

	@Test
	public void deleteById() {
		final IdTestEntity<ID> entity = new IdTestEntity<>();
		repository.save(entity);
		assertThat(repository.count(), is(1L));
		repository.deleteById(entity.getId());
		assertThat(repository.count(), is(0L));
	}

	@Test
	public void deleteAllById() {
		final IdTestEntity<ID> e1 = new IdTestEntity<>();
		final IdTestEntity<ID> e2 = new IdTestEntity<>();
		repository.save(e1);
		repository.save(e2);
		assertThat(repository.count(), is(2L));
		repository.deleteAllById(Arrays.asList(e1.getId(), e2.getId()));
		assertThat(repository.count(), is(0L));
	}

	@Test
	public void existsById() {
		final IdTestEntity<ID> entity = new IdTestEntity<>();
		repository.save(entity);
		assertThat(repository.existsById(entity.getId()), is(true));
	}

	@Test
	public void findAllById() {
		final IdTestEntity<ID> entity1 = new IdTestEntity<>();
		final IdTestEntity<ID> entity2 = new IdTestEntity<>();
		repository.saveAll(Arrays.asList(entity1, entity2));
		final Iterable<IdTestEntity<ID>> find = repository.findAllById(Arrays.asList(entity1.getId(), entity2.getId()));
		assertThat(StreamSupport.stream(find.spliterator(), false).count(), is(2L));
	}

	@Test
	public void findById() {
		final IdTestEntity<ID> entity = new IdTestEntity<>();
		repository.save(entity);
		final Optional<IdTestEntity<ID>> find = repository.findById(entity.getId());
		assertThat(find.isPresent(), is(true));
	}

	@Test
	public void save() {
		repository.save(new IdTestEntity<>());
		assertThat(repository.count(), is(1L));
	}

	@Test
	public void saveAll() {
		repository.saveAll(Arrays.asList(new IdTestEntity<>(), new IdTestEntity<>()));
		assertThat(repository.count(), is(2L));
	}

	@Test
	public void query() {
		final IdTestEntity<ID> entity = new IdTestEntity<>();
		repository.save(entity);
		final Optional<IdTestEntity<ID>> find = repository.findByQuery(entity.getId());
		assertThat(find.isPresent(), is(true));
		assertThat(find.get().getId(), is(entity.getId()));
	}

	@Test
	public void queryId() {
		final IdTestEntity<ID> entity = new IdTestEntity<>();
		repository.save(entity);
		final Optional<ID> find = repository.findIdByQuery(entity.getId());
		assertThat(find.isPresent(), is(true));
		assertThat(find.get(), is(entity.getId()));
	}

	@Test
	public void queryEntity() {
		final IdTestEntity<ID> entity = new IdTestEntity<>();
		repository.save(entity);
		final Optional<IdTestEntity<ID>> find = repository.findByEntity(entity);
		assertThat(find.isPresent(), is(true));
		assertThat(find.get().getId(), is(entity.getId()));
	}

}
