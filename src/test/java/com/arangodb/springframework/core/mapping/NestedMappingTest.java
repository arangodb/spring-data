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

package com.arangodb.springframework.core.mapping;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.core.mapping.testdata.BasicTestEntity;

/**
 * @author Mark Vollmary
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class NestedMappingTest extends AbstractArangoTest {

	public static class SingleNestedDocumentTestEntity extends BasicTestEntity {
		private NestedDocumentTestEntity entity;
	}

	public static class NestedDocumentTestEntity {
		private String test;

		public NestedDocumentTestEntity() {
			super();
		}

		public NestedDocumentTestEntity(final String test) {
			super();
			this.test = test;
		}
	}

	@Test
	public void singleNestedDocument() {
		final SingleNestedDocumentTestEntity entity = new SingleNestedDocumentTestEntity();
		entity.entity = new NestedDocumentTestEntity("test");
		template.insert(entity);
		final SingleNestedDocumentTestEntity document = template.find(entity.id, SingleNestedDocumentTestEntity.class)
				.get();
		assertThat(document, is(notNullValue()));
		assertThat(document.entity, is(notNullValue()));
		assertThat(document.entity.test, is("test"));
	}

	public static class MultipleNestedDocumentTestEntity extends BasicTestEntity {
		private Collection<NestedDocumentTestEntity> entities;
	}

	@Test
	public void multipleNestedDocuments() {
		final MultipleNestedDocumentTestEntity entity = new MultipleNestedDocumentTestEntity();
		entity.entities = new ArrayList<>(Arrays.asList(new NestedDocumentTestEntity("0"),
			new NestedDocumentTestEntity("1"), new NestedDocumentTestEntity("2")));
		template.insert(entity);
		final MultipleNestedDocumentTestEntity document = template
				.find(entity.id, MultipleNestedDocumentTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		assertThat(document.entities.size(), is(3));
		assertThat(document.entities.stream().map(e -> e.test).collect(Collectors.toList()), hasItems("0", "1", "2"));
	}

	public static class MultipleNestedCollectionsTestEntity extends BasicTestEntity {
		private Collection<Collection<NestedDocumentTestEntity>> entities;
	}

	@Test
	public void multipleNestedCollections() {
		final MultipleNestedCollectionsTestEntity entity = new MultipleNestedCollectionsTestEntity();
		entity.entities = new ArrayList<>(Arrays.asList(
			new ArrayList<>(Arrays.asList(new NestedDocumentTestEntity("00"), new NestedDocumentTestEntity("01"),
				new NestedDocumentTestEntity("02"))),
			new ArrayList<>(Arrays.asList(new NestedDocumentTestEntity("10"), new NestedDocumentTestEntity("11"),
				new NestedDocumentTestEntity("12")))));
		template.insert(entity);
		final MultipleNestedCollectionsTestEntity document = template
				.find(entity.id, MultipleNestedCollectionsTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		assertThat(document.entities.size(), is(2));
		final List<List<String>> collect = document.entities.stream()
				.map(c -> c.stream().map(cc -> cc.test).collect(Collectors.toList())).collect(Collectors.toList());
		for (int i = 0; i < collect.size(); i++) {
			for (int j = 0; j < collect.get(i).size(); j++) {
				assertThat(collect.get(i).get(j), is(i + "" + j));
			}
		}
	}

	public static class SingleNestedMapTestEntity extends BasicTestEntity {
		private Map<String, NestedDocumentTestEntity> entities;
	}

	@Test
	public void singleNestedMap() {
		final SingleNestedMapTestEntity entity = new SingleNestedMapTestEntity();
		entity.entities = new HashMap<>();
		entity.entities.put("0", new NestedDocumentTestEntity("0"));
		entity.entities.put("1", new NestedDocumentTestEntity("1"));
		entity.entities.put("2", new NestedDocumentTestEntity("2"));
		template.insert(entity);
		final SingleNestedMapTestEntity document = template.find(entity.id, SingleNestedMapTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		final Map<String, String> collect = document.entities.entrySet().stream()
				.map(e -> new String[] { e.getKey(), e.getValue().test })
				.collect(Collectors.toMap(k -> k[0], v -> v[1]));
		for (int i = 0; i <= 2; i++) {
			assertThat(collect.get(String.valueOf(i)), is(String.valueOf(i)));
		}
	}

	public static class MultipleNestedMapTestEntity extends BasicTestEntity {
		private Map<String, Map<String, NestedDocumentTestEntity>> entities;
	}

	@Test
	public void multipleNestedMaps() {
		final MultipleNestedMapTestEntity entity = new MultipleNestedMapTestEntity();
		entity.entities = new HashMap<>();
		final Map<String, NestedDocumentTestEntity> m0 = new HashMap<>();
		m0.put("0", new NestedDocumentTestEntity("00"));
		m0.put("1", new NestedDocumentTestEntity("01"));
		m0.put("2", new NestedDocumentTestEntity("02"));
		entity.entities.put("0", m0);
		final Map<String, NestedDocumentTestEntity> m1 = new HashMap<>();
		m1.put("0", new NestedDocumentTestEntity("10"));
		m1.put("1", new NestedDocumentTestEntity("11"));
		m1.put("2", new NestedDocumentTestEntity("12"));
		entity.entities.put("1", m1);
		template.insert(entity);
		final MultipleNestedMapTestEntity document = template.find(entity.id, MultipleNestedMapTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		for (int i = 0; i <= 1; i++) {
			for (int j = 0; j <= 2; j++) {
				assertThat(document.entities.get(String.valueOf(i)).get(String.valueOf(j)).test, is(i + "" + j));
			}
		}
	}

	@Document("sameCollection")
	static class TwoTypesInSameCollectionA extends BasicTestEntity {
		String value;
		String a;
	}

	@Document("sameCollection")
	static class TwoTypesInSameCollectionB extends BasicTestEntity {
		String value;
		String b;
	}

	static class SameCollectionTestEntity extends BasicTestEntity {
		Collection<Object> value;
	}

	@Test
	public void twoTypesInSameCollection() {
		final TwoTypesInSameCollectionA a = new TwoTypesInSameCollectionA();
		a.value = "testA";
		a.a = "testA";
		final TwoTypesInSameCollectionB b = new TwoTypesInSameCollectionB();
		b.value = "testB";
		b.b = "testB";
		final SameCollectionTestEntity c = new SameCollectionTestEntity();
		c.value = new ArrayList<>();
		c.value.add(a);
		c.value.add(b);

		template.insert(c);
		final Optional<SameCollectionTestEntity> findC = template.find(c.getId(), SameCollectionTestEntity.class);
		assertThat(findC.isPresent(), is(true));
		final Collection<Object> value = findC.get().value;
		assertThat(value.size(), is(2));
		{
			assertThat(value.stream().filter(v -> v instanceof TwoTypesInSameCollectionA).count(), is(1L));
			final Optional<Object> findA = value.stream().filter(v -> v instanceof TwoTypesInSameCollectionA)
					.findFirst();
			assertThat(findA.isPresent(), is(true));
			final TwoTypesInSameCollectionA aa = (TwoTypesInSameCollectionA) findA.get();
			assertThat(aa.value, is("testA"));
			assertThat(aa.a, is("testA"));
		}
		{
			assertThat(value.stream().filter(v -> v instanceof TwoTypesInSameCollectionB).count(), is(1L));
			final Optional<Object> findB = value.stream().filter(v -> v instanceof TwoTypesInSameCollectionB)
					.findFirst();
			assertThat(findB.isPresent(), is(true));
			final TwoTypesInSameCollectionB bb = (TwoTypesInSameCollectionB) findB.get();
			assertThat(bb.value, is("testB"));
			assertThat(bb.b, is("testB"));
		}
	}
}
