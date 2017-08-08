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

package com.arangodb.springframework.core.mapping;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.Field;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.Key;
import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.annotation.Rev;
import com.arangodb.springframework.annotation.To;
import com.arangodb.velocypack.VPackSlice;

/**
 * @author Mark Vollmary
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class ArangoMappingTest extends AbstractArangoTest {

	@Document
	public static class BasicTestEntity {

		@Id
		String id;
		@Key
		String key;
		@Rev
		String rev;

		public BasicTestEntity() {
			super();
		}

		public String getId() {
			return id;
		}

		public void setId(final String id) {
			this.id = id;
		}

		public String getKey() {
			return key;
		}

		public void setKey(final String key) {
			this.key = key;
		}

		public String getRev() {
			return rev;
		}

		public void setRev(final String rev) {
			this.rev = rev;
		}

	}

	@Test
	public void idKeyRev() {
		final DocumentCreateEntity<BasicTestEntity> ref = template.insertDocument(new BasicTestEntity());
		final BasicTestEntity entity = template.getDocument(ref.getId(), BasicTestEntity.class);
		assertThat(entity, is(notNullValue()));
		assertThat(entity.getId(), is(ref.getId()));
		assertThat(entity.getKey(), is(ref.getKey()));
		assertThat(entity.getRev(), is(ref.getRev()));
	}

	public static class FieldNameTestEntity extends BasicTestEntity {
		@Field("alt-test")
		private String test;
	}

	@Test
	public void fieldNameAnnotation() {
		final FieldNameTestEntity entity = new FieldNameTestEntity();
		entity.test = "1234";
		final DocumentCreateEntity<FieldNameTestEntity> res = template.insertDocument(entity);
		final VPackSlice slice = template.driver().db(ArangoTestConfiguration.DB).getDocument(res.getId(),
			VPackSlice.class);
		assertThat(slice, is(notNullValue()));
		assertThat(slice.get("alt-test").isString(), is(true));
		assertThat(slice.get("alt-test").getAsString(), is(entity.test));
	}

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
		template.insertDocument(entity);
		final SingleNestedDocumentTestEntity document = template.getDocument(entity.id,
			SingleNestedDocumentTestEntity.class);
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
		template.insertDocument(entity);
		final MultipleNestedDocumentTestEntity document = template.getDocument(entity.id,
			MultipleNestedDocumentTestEntity.class);
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
		template.insertDocument(entity);
		final MultipleNestedCollectionsTestEntity document = template.getDocument(entity.id,
			MultipleNestedCollectionsTestEntity.class);
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
		template.insertDocument(entity);
		final SingleNestedMapTestEntity document = template.getDocument(entity.id, SingleNestedMapTestEntity.class);
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
		template.insertDocument(entity);
		final MultipleNestedMapTestEntity document = template.getDocument(entity.id, MultipleNestedMapTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		for (int i = 0; i <= 1; i++) {
			for (int j = 0; j <= 2; j++) {
				assertThat(document.entities.get(String.valueOf(i)).get(String.valueOf(j)).test, is(i + "" + j));
			}
		}
	}

	public static class SingleReferenceTestEntity extends BasicTestEntity {
		@Ref
		private BasicTestEntity entity;
	}

	@Test
	public void singleRef() {
		final BasicTestEntity e1 = new BasicTestEntity();
		template.insertDocument(e1);
		final SingleReferenceTestEntity e0 = new SingleReferenceTestEntity();
		e0.entity = e1;
		template.insertDocument(e0);
		final SingleReferenceTestEntity document = template.getDocument(e0.id, SingleReferenceTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.entity, is(notNullValue()));
		assertThat(document.entity.id, is(e1.id));
	}

	public static class SingleReferenceLazyTestEntity extends BasicTestEntity {
		@Ref(lazy = true)
		private BasicTestEntity entity;
	}

	@Test
	public void singleRefLazy() {
		final BasicTestEntity e1 = new BasicTestEntity();
		template.insertDocument(e1);
		final SingleReferenceLazyTestEntity e0 = new SingleReferenceLazyTestEntity();
		e0.entity = e1;
		template.insertDocument(e0);
		final SingleReferenceLazyTestEntity document = template.getDocument(e0.id, SingleReferenceLazyTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.entity, is(notNullValue()));
		assertThat(document.entity, instanceOf(BasicTestEntity.class));
		assertThat(document.entity.getId(), is(e1.getId()));
	}

	public static class MultiReferenceTestEntity extends BasicTestEntity {
		@Ref
		private Collection<BasicTestEntity> entities;
	}

	@Test
	public void multiRef() {
		final BasicTestEntity e1 = new BasicTestEntity();
		template.insertDocument(e1);
		final BasicTestEntity e2 = new BasicTestEntity();
		template.insertDocument(e2);
		final MultiReferenceTestEntity e0 = new MultiReferenceTestEntity();
		e0.entities = Arrays.asList(e1, e2);
		template.insertDocument(e0);
		final MultiReferenceTestEntity document = template.getDocument(e0.id, MultiReferenceTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		assertThat(document.entities.size(), is(2));
		for (final BasicTestEntity e : document.entities) {
			assertThat(e, instanceOf(BasicTestEntity.class));
			assertThat(e.getId(), is(notNullValue()));
			assertThat(e.getId(), is(isOneOf(e1.getId(), e2.getId())));
		}
	}

	public static class MultiReferenceLazyTestEntity extends BasicTestEntity {
		@Ref(lazy = true)
		private Collection<BasicTestEntity> entities;
	}

	@Test
	public void multiRefLazy() {
		final BasicTestEntity e1 = new BasicTestEntity();
		template.insertDocument(e1);
		final BasicTestEntity e2 = new BasicTestEntity();
		template.insertDocument(e2);
		final MultiReferenceLazyTestEntity e0 = new MultiReferenceLazyTestEntity();
		e0.entities = Arrays.asList(e1, e2);
		template.insertDocument(e0);
		final MultiReferenceLazyTestEntity document = template.getDocument(e0.id, MultiReferenceLazyTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		assertThat(document.entities.size(), is(2));
		for (final BasicTestEntity e : document.entities) {
			assertThat(e, instanceOf(BasicTestEntity.class));
			assertThat(e.getId(), is(notNullValue()));
			assertThat(e.getId(), is(isOneOf(e1.getId(), e2.getId())));
		}
	}

	@Edge
	public static class BasicEdgeTestEntity extends BasicTestEntity {
		@From
		BasicTestEntity from;
		@To
		BasicTestEntity to;

		public BasicEdgeTestEntity() {
			super();
		}

		public BasicEdgeTestEntity(final BasicTestEntity from, final BasicTestEntity to) {
			super();
			this.from = from;
			this.to = to;
		}

		public BasicTestEntity getFrom() {
			return from;
		}

		public void setFrom(final BasicTestEntity from) {
			this.from = from;
		}

		public BasicTestEntity getTo() {
			return to;
		}

		public void setTo(final BasicTestEntity to) {
			this.to = to;
		}

	}

	@Test
	public void edgeFromTo() {
		final BasicTestEntity e1 = new BasicTestEntity();
		template.insertDocument(e1);
		final BasicTestEntity e2 = new BasicTestEntity();
		template.insertDocument(e2);
		final BasicEdgeTestEntity e0 = new BasicEdgeTestEntity(e1, e2);
		template.insertDocument(e0);
		final BasicEdgeTestEntity document = template.getDocument(e0.id, BasicEdgeTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.getFrom(), is(notNullValue()));
		assertThat(document.getFrom().getId(), is(e1.getId()));
		assertThat(document.getTo(), is(notNullValue()));
		assertThat(document.getTo().getId(), is(e2.getId()));
	}

	@Edge
	public static class BasicEdgeLazyTestEntity extends BasicTestEntity {
		@From(lazy = true)
		BasicTestEntity from;
		@To(lazy = true)
		BasicTestEntity to;

		public BasicEdgeLazyTestEntity() {
			super();
		}

		public BasicEdgeLazyTestEntity(final BasicTestEntity from, final BasicTestEntity to) {
			super();
			this.from = from;
			this.to = to;
		}

		public BasicTestEntity getFrom() {
			return from;
		}

		public void setFrom(final BasicTestEntity from) {
			this.from = from;
		}

		public BasicTestEntity getTo() {
			return to;
		}

		public void setTo(final BasicTestEntity to) {
			this.to = to;
		}

	}

	@Test
	public void edgeFromToLazy() {
		final BasicTestEntity e1 = new BasicTestEntity();
		template.insertDocument(e1);
		final BasicTestEntity e2 = new BasicTestEntity();
		template.insertDocument(e2);
		final BasicEdgeLazyTestEntity e0 = new BasicEdgeLazyTestEntity(e1, e2);
		template.insertDocument(e0);
		final BasicEdgeLazyTestEntity document = template.getDocument(e0.id, BasicEdgeLazyTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.getFrom(), is(notNullValue()));
		assertThat(document.getFrom().getId(), is(e1.getId()));
		assertThat(document.getTo(), is(notNullValue()));
		assertThat(document.getTo().getId(), is(e2.getId()));
	}

	public static class DocumentFromTestEntity extends BasicTestEntity {
		@From
		private Collection<BasicEdgeLazyTestEntity> entities;
	}

	@Test
	public void documentFrom() {
		final DocumentFromTestEntity e0 = new DocumentFromTestEntity();
		template.insertDocument(e0);
		final DocumentFromTestEntity e1 = new DocumentFromTestEntity();
		template.insertDocument(e1);
		final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e0, e1);
		template.insertDocument(edge0);
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e0, e1);
		template.insertDocument(edge1);
		final DocumentFromTestEntity document = template.getDocument(e0.id, DocumentFromTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		assertThat(document.entities.size(), is(2));
		for (final BasicEdgeLazyTestEntity e : document.entities) {
			assertThat(e, instanceOf(BasicEdgeLazyTestEntity.class));
			assertThat(e.getId(), is(notNullValue()));
			assertThat(e.getId(), is(isOneOf(edge0.getId(), edge1.getId())));
			assertThat(e.getFrom(), is(notNullValue()));
			assertThat(e.getFrom().getId(), is(notNullValue()));
			assertThat(e.getFrom().getId(), is(e0.getId()));
		}
	}

	public static class DocumentFromLazyTestEntity extends BasicTestEntity {
		@From(lazy = true)
		private Collection<BasicEdgeLazyTestEntity> entities;
	}

	@Test
	public void documentFromLazy() {
		final DocumentFromLazyTestEntity e0 = new DocumentFromLazyTestEntity();
		template.insertDocument(e0);
		final DocumentFromLazyTestEntity e1 = new DocumentFromLazyTestEntity();
		template.insertDocument(e1);
		final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e0, e1);
		template.insertDocument(edge0);
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e0, e1);
		template.insertDocument(edge1);
		final DocumentFromLazyTestEntity document = template.getDocument(e0.id, DocumentFromLazyTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		assertThat(document.entities.size(), is(2));
		for (final BasicEdgeLazyTestEntity e : document.entities) {
			assertThat(e, instanceOf(BasicEdgeLazyTestEntity.class));
			assertThat(e.getId(), is(notNullValue()));
			assertThat(e.getId(), is(isOneOf(edge0.getId(), edge1.getId())));
			assertThat(e.getFrom(), is(notNullValue()));
			assertThat(e.getFrom().getId(), is(notNullValue()));
			assertThat(e.getFrom().getId(), is(e0.getId()));
		}
	}

	public static class DocumentToTestEntity extends BasicTestEntity {
		@To
		private Collection<BasicEdgeLazyTestEntity> entities;
	}

	@Test
	public void documentTo() {
		final DocumentToTestEntity e0 = new DocumentToTestEntity();
		template.insertDocument(e0);
		final DocumentToTestEntity e1 = new DocumentToTestEntity();
		template.insertDocument(e1);
		final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e1, e0);
		template.insertDocument(edge0);
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e1, e0);
		template.insertDocument(edge1);
		final DocumentToTestEntity document = template.getDocument(e0.id, DocumentToTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		assertThat(document.entities.size(), is(2));
		for (final BasicEdgeLazyTestEntity e : document.entities) {
			assertThat(e, instanceOf(BasicEdgeLazyTestEntity.class));
			assertThat(e.getId(), is(notNullValue()));
			assertThat(e.getId(), is(isOneOf(edge0.getId(), edge1.getId())));
			assertThat(e.getTo(), is(notNullValue()));
			assertThat(e.getTo().getId(), is(notNullValue()));
			assertThat(e.getTo().getId(), is(e0.getId()));
		}
	}

	public static class DocumentToLazyTestEntity extends BasicTestEntity {
		@To(lazy = true)
		private Collection<BasicEdgeLazyTestEntity> entities;
	}

	@Test
	public void documentToLazy() {
		final DocumentToLazyTestEntity e0 = new DocumentToLazyTestEntity();
		template.insertDocument(e0);
		final DocumentToLazyTestEntity e1 = new DocumentToLazyTestEntity();
		template.insertDocument(e1);
		final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e1, e0);
		template.insertDocument(edge0);
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e1, e0);
		template.insertDocument(edge1);
		final DocumentToLazyTestEntity document = template.getDocument(e0.id, DocumentToLazyTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		assertThat(document.entities.size(), is(2));
		for (final BasicEdgeLazyTestEntity e : document.entities) {
			assertThat(e, instanceOf(BasicEdgeLazyTestEntity.class));
			assertThat(e.getId(), is(notNullValue()));
			assertThat(e.getId(), is(isOneOf(edge0.getId(), edge1.getId())));
			assertThat(e.getTo(), is(notNullValue()));
			assertThat(e.getTo().getId(), is(notNullValue()));
			assertThat(e.getTo().getId(), is(e0.getId()));
		}
	}

	public static class RelationsTestEntity extends BasicTestEntity {
		@Relations(edge = BasicEdgeTestEntity.class)
		private Collection<BasicTestEntity> entities;
	}

	@Test
	public void relations() {
		final BasicTestEntity e1 = new BasicTestEntity();
		template.insertDocument(e1);
		final BasicTestEntity e2 = new BasicTestEntity();
		template.insertDocument(e2);
		final RelationsTestEntity e0 = new RelationsTestEntity();
		template.insertDocument(e0);
		template.insertDocument(new BasicEdgeTestEntity(e0, e1));
		template.insertDocument(new BasicEdgeTestEntity(e0, e2));

		final RelationsTestEntity document = template.getDocument(e0.id, RelationsTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		assertThat(document.entities.size(), is(2));
		for (final BasicTestEntity e : document.entities) {
			assertThat(e, instanceOf(BasicTestEntity.class));
			assertThat(e.getId(), is(notNullValue()));
			assertThat(e.getId(), is(isOneOf(e1.getId(), e2.getId())));
		}
	}

	public static class RelationsLazyTestEntity extends BasicTestEntity {
		@Relations(edge = BasicEdgeTestEntity.class, lazy = true)
		private Collection<BasicTestEntity> entities;
	}

	@Test
	public void relationsLazy() {
		final BasicTestEntity e1 = new BasicTestEntity();
		template.insertDocument(e1);
		final BasicTestEntity e2 = new BasicTestEntity();
		template.insertDocument(e2);
		final RelationsTestEntity e0 = new RelationsTestEntity();
		template.insertDocument(e0);
		template.insertDocument(new BasicEdgeTestEntity(e0, e1));
		template.insertDocument(new BasicEdgeTestEntity(e0, e2));

		final RelationsLazyTestEntity document = template.getDocument(e0.id, RelationsLazyTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		assertThat(document.entities.size(), is(2));
		for (final BasicTestEntity e : document.entities) {
			assertThat(e, instanceOf(BasicTestEntity.class));
			assertThat(e.getId(), is(notNullValue()));
			assertThat(e.getId(), is(isOneOf(e1.getId(), e2.getId())));
		}
	}

	public static class ConstructorWithParamTestEntity extends BasicTestEntity {
		private final String value;

		public ConstructorWithParamTestEntity(final String value) {
			super();
			this.value = value;
		}
	}

	@Test
	public void constructorWithParam() {
		final ConstructorWithParamTestEntity entity = new ConstructorWithParamTestEntity("test");
		template.insertDocument(entity);
		final ConstructorWithParamTestEntity document = template.getDocument(entity.getId(),
			ConstructorWithParamTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(entity.value));
	}

	public static class ConstructorWithMultipleParamsTestEntity extends BasicTestEntity {
		private final String value1;
		private final boolean value2;
		private final double value3;

		public ConstructorWithMultipleParamsTestEntity(final String value1, final boolean value2, final double value3) {
			super();
			this.value1 = value1;
			this.value2 = value2;
			this.value3 = value3;
		}

	}

	@Test
	public void constructorWithMultipleParams() {
		final ConstructorWithMultipleParamsTestEntity entity = new ConstructorWithMultipleParamsTestEntity("test", true,
				3.5);
		template.insertDocument(entity);
		final ConstructorWithMultipleParamsTestEntity document = template.getDocument(entity.getId(),
			ConstructorWithMultipleParamsTestEntity.class);
		assertThat(document, is(notNullValue()));
		assertThat(document.value1, is(entity.value1));
		assertThat(document.value2, is(entity.value2));
		assertThat(document.value3, is(entity.value3));
	}

}
