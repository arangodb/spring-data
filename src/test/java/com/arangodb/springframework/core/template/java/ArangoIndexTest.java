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

package com.arangodb.springframework.core.template.java;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.entity.IndexType;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.annotation.HashIndex;
import com.arangodb.springframework.annotation.HashIndexed;
import com.arangodb.springframework.annotation.HashIndexes;

/**
 * @author Mark Vollmary
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class ArangoIndexTest extends AbstractArangoTest {

	public static class HashIndexedSingleFieldTestEntity {
		@HashIndexed
		private String a;
	}

	@Test
	public void singleFieldHashIndexed() {
		assertThat(template.collection(HashIndexedSingleFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(HashIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash));
		assertThat(template.collection(HashIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.hash).findFirst().get().getFields(),
			hasItems("a"));
	}

	public static class HashIndexedMultipleSingleFieldTestEntity {
		@HashIndexed
		private String a;
		@HashIndexed
		private String b;
	}

	@Test
	public void multipleSingleFieldHashIndexed() {
		assertThat(template.collection(HashIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(template.collection(HashIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash));
	}

	@HashIndex(fields = { "a" })
	public static class HashIndexWithSingleFieldTestEntity {
	}

	@Test
	public void singleFieldHashIndex() {
		assertThat(template.collection(HashIndexWithSingleFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(HashIndexWithSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash));
		assertThat(template.collection(HashIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.hash).findFirst().get().getFields(),
			hasItems("a"));
	}

	@HashIndex(fields = { "a" })
	@HashIndex(fields = { "b" })
	public static class HashIndexWithMultipleSingleFieldTestEntity {
	}

	@Test
	public void multipleSingleFieldHashIndex() {
		assertThat(template.collection(HashIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(template.collection(HashIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash));
	}

	@HashIndex(fields = { "a", "b" })
	public static class HashIndexWithMultiFieldTestEntity {
	}

	@Test
	public void multiFieldHashIndex() {
		assertThat(template.collection(HashIndexWithMultiFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(HashIndexWithMultiFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash));
		assertThat(template.collection(HashIndexWithMultiFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.hash).findFirst().get().getFields(),
			hasItems("a", "b"));
	}

	@HashIndexes({ @HashIndex(fields = { "a" }), @HashIndex(fields = { "b" }) })
	public static class HashIndexWithMultipleIndexesTestEntity {
	}

	@Test
	public void multipleIndexesHashIndex() {
		assertThat(template.collection(HashIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(template.collection(HashIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash));
	}

}
