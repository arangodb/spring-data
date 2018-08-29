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
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.testdata.ArangoSearchTestEntity;

/**
 * @author Mark Vollmary
 *
 */

public class ArangoSearchRepositoryTest extends AbstractArangoTest {

	@Autowired
	ArangoOperations operations;

	@Autowired
	ArangoSearchTestRepo repository;

	public ArangoSearchRepositoryTest() {
		super();
	}

	@Test
	public void findById() {
		final ArangoSearchTestEntity entity = new ArangoSearchTestEntity("test");
		operations.insert(entity);
		final Optional<ArangoSearchTestEntity> find = repository.findById(entity.getId());
		assertThat(find.isPresent(), is(true));
		assertThat(find.get().getValue(), is("test"));
	}

	@Test
	public void query() throws InterruptedException {
		final ArangoSearchTestEntity entity = new ArangoSearchTestEntity("test");
		operations.insert(entity);
		Thread.sleep(1000);
		final List<ArangoSearchTestEntity> find = repository.findByValue("test");
		assertThat(find.size(), is(1));
		assertThat(find.get(0).getValue(), is("test"));
	}

	@Test
	public void findAll() throws InterruptedException {
		final ArangoSearchTestEntity entity = new ArangoSearchTestEntity("test");
		operations.insert(entity);
		Thread.sleep(1000);
		final Iterable<ArangoSearchTestEntity> find = repository.findAll();
		assertThat(find.iterator().next().getValue(), is("test"));
		assertThat(find.iterator().hasNext(), is(false));
	}

	@Test
	public void findAllById() throws InterruptedException {
		final ArangoSearchTestEntity entity = new ArangoSearchTestEntity("test");
		operations.insert(entity);
		Thread.sleep(1000);
		final Iterable<ArangoSearchTestEntity> find = repository.findAllById(Arrays.asList(entity.getId()));
		assertThat(find.iterator().next().getValue(), is("test"));
		assertThat(find.iterator().hasNext(), is(false));
	}

	@Test
	public void count() throws InterruptedException {
		final ArangoSearchTestEntity entity = new ArangoSearchTestEntity("test");
		operations.insert(entity);
		Thread.sleep(1000);
		final long count = repository.count();
		assertThat(count, is(1L));
	}
}
