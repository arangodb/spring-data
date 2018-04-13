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
import static org.hamcrest.Matchers.nullValue;
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

import com.arangodb.entity.DocumentEntity;
import com.arangodb.model.AqlQueryOptions;
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
import com.arangodb.util.MapBuilder;
import com.arangodb.velocypack.VPackSlice;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
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
		final DocumentEntity ref = template.insert(new BasicTestEntity());
		final BasicTestEntity entity = template.find(ref.getId(), BasicTestEntity.class).get();
		assertThat(entity, is(notNullValue()));
		assertThat(entity.getId(), is(ref.getId()));
		assertThat(entity.getKey(), is(ref.getKey()));
		assertThat(entity.getRev(), is(ref.getRev()));
	}

	public static class OnlyIdTestEntity {
		@Id
		private String id;
	}

	@Test
	public void supplementKey() {
		final OnlyIdTestEntity value = new OnlyIdTestEntity();
		template.insert(value);
		final List<BasicTestEntity> result = template.query("RETURN @doc", new MapBuilder().put("doc", value).get(),
			new AqlQueryOptions(), BasicTestEntity.class).asListRemaining();
		assertThat(result.size(), is(1));
		assertThat(result.get(0).getId(), is(value.id));
		assertThat(result.get(0).getKey(), is(value.id.split("/")[1]));
		assertThat(result.get(0).getRev(), is(nullValue()));
	}

	public static class FieldNameTestEntity extends BasicTestEntity {
		@Field("alt-test")
		private String test;
	}

	@Test
	public void fieldNameAnnotation() {
		final FieldNameTestEntity entity = new FieldNameTestEntity();
		entity.test = "1234";
		final DocumentEntity res = template.insert(entity);
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

	public static class SingleReferenceTestEntity extends BasicTestEntity {
		@Ref
		private BasicTestEntity entity;
	}

	@Test
	public void singleRef() {
		final BasicTestEntity e1 = new BasicTestEntity();
		template.insert(e1);
		final SingleReferenceTestEntity e0 = new SingleReferenceTestEntity();
		e0.entity = e1;
		template.insert(e0);
		final SingleReferenceTestEntity document = template.find(e0.id, SingleReferenceTestEntity.class).get();
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
		template.insert(e1);
		final SingleReferenceLazyTestEntity e0 = new SingleReferenceLazyTestEntity();
		e0.entity = e1;
		template.insert(e0);
		final SingleReferenceLazyTestEntity document = template.find(e0.id, SingleReferenceLazyTestEntity.class).get();
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
		template.insert(e1);
		final BasicTestEntity e2 = new BasicTestEntity();
		template.insert(e2);
		final MultiReferenceTestEntity e0 = new MultiReferenceTestEntity();
		e0.entities = Arrays.asList(e1, e2);
		template.insert(e0);
		final MultiReferenceTestEntity document = template.find(e0.id, MultiReferenceTestEntity.class).get();
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
		template.insert(e1);
		final BasicTestEntity e2 = new BasicTestEntity();
		template.insert(e2);
		final MultiReferenceLazyTestEntity e0 = new MultiReferenceLazyTestEntity();
		e0.entities = Arrays.asList(e1, e2);
		template.insert(e0);
		final MultiReferenceLazyTestEntity document = template.find(e0.id, MultiReferenceLazyTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		assertThat(document.entities.size(), is(2));
		for (final BasicTestEntity e : document.entities) {
			assertThat(e, instanceOf(BasicTestEntity.class));
			assertThat(e.getId(), is(notNullValue()));
			assertThat(e.getId(), is(isOneOf(e1.getId(), e2.getId())));
		}
	}

	public static class NestedReferenceTestEntity extends BasicTestEntity {
		private NestedReferenceSubTestEntity sub;
	}

	public static class NestedReferenceSubTestEntity {
		@Ref
		private Collection<BasicTestEntity> entities;
	}

	@Test
	public void testNestedRef() {
		final NestedReferenceTestEntity o = new NestedReferenceTestEntity();
		o.sub = new NestedReferenceSubTestEntity();
		o.sub.entities = new ArrayList<>();
		final BasicTestEntity e = new BasicTestEntity();
		o.sub.entities.add(e);
		template.insert(e);
		template.insert(o);
		final NestedReferenceTestEntity document = template.find(o.id, NestedReferenceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.sub, is(notNullValue()));
		assertThat(document.sub.entities, is(notNullValue()));
		assertThat(document.sub.entities.size(), is(1));
		assertThat(document.sub.entities.iterator().next().id, is(e.id));
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
		template.insert(e1);
		final BasicTestEntity e2 = new BasicTestEntity();
		template.insert(e2);
		final BasicEdgeTestEntity e0 = new BasicEdgeTestEntity(e1, e2);
		template.insert(e0);
		final BasicEdgeTestEntity document = template.find(e0.id, BasicEdgeTestEntity.class).get();
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
		template.insert(e1);
		final BasicTestEntity e2 = new BasicTestEntity();
		template.insert(e2);
		final BasicEdgeLazyTestEntity e0 = new BasicEdgeLazyTestEntity(e1, e2);
		template.insert(e0);
		final BasicEdgeLazyTestEntity document = template.find(e0.id, BasicEdgeLazyTestEntity.class).get();
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
		template.insert(e0);
		final DocumentFromTestEntity e1 = new DocumentFromTestEntity();
		template.insert(e1);
		final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e0, e1);
		template.insert(edge0);
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e0, e1);
		template.insert(edge1);
		final DocumentFromTestEntity document = template.find(e0.id, DocumentFromTestEntity.class).get();
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
		template.insert(e0);
		final DocumentFromLazyTestEntity e1 = new DocumentFromLazyTestEntity();
		template.insert(e1);
		final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e0, e1);
		template.insert(edge0);
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e0, e1);
		template.insert(edge1);
		final DocumentFromLazyTestEntity document = template.find(e0.id, DocumentFromLazyTestEntity.class).get();
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
		template.insert(e0);
		final DocumentToTestEntity e1 = new DocumentToTestEntity();
		template.insert(e1);
		final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e1, e0);
		template.insert(edge0);
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e1, e0);
		template.insert(edge1);
		final DocumentToTestEntity document = template.find(e0.id, DocumentToTestEntity.class).get();
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
		template.insert(e0);
		final DocumentToLazyTestEntity e1 = new DocumentToLazyTestEntity();
		template.insert(e1);
		final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e1, e0);
		template.insert(edge0);
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e1, e0);
		template.insert(edge1);
		final DocumentToLazyTestEntity document = template.find(e0.id, DocumentToLazyTestEntity.class).get();
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
		@Relations(edges = BasicEdgeTestEntity.class)
		private Collection<BasicTestEntity> entities;
	}

	@Test
	public void relations() {
		final BasicTestEntity e1 = new BasicTestEntity();
		template.insert(e1);
		final BasicTestEntity e2 = new BasicTestEntity();
		template.insert(e2);
		final RelationsTestEntity e0 = new RelationsTestEntity();
		template.insert(e0);
		template.insert(new BasicEdgeTestEntity(e0, e1));
		template.insert(new BasicEdgeTestEntity(e0, e2));

		final RelationsTestEntity document = template.find(e0.id, RelationsTestEntity.class).get();
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
		@Relations(edges = BasicEdgeTestEntity.class, lazy = true)
		private Collection<BasicTestEntity> entities;
	}

	@Test
	public void relationsLazy() {
		final BasicTestEntity e1 = new BasicTestEntity();
		template.insert(e1);
		final BasicTestEntity e2 = new BasicTestEntity();
		template.insert(e2);
		final RelationsTestEntity e0 = new RelationsTestEntity();
		template.insert(e0);
		template.insert(new BasicEdgeTestEntity(e0, e1));
		template.insert(new BasicEdgeTestEntity(e0, e2));

		final RelationsLazyTestEntity document = template.find(e0.id, RelationsLazyTestEntity.class).get();
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
		template.insert(entity);
		final ConstructorWithParamTestEntity document = template
				.find(entity.getId(), ConstructorWithParamTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(entity.value));
	}

	public static class ConstructorWithMultipleParamsTestEntity extends BasicTestEntity {
		private final String value1;
		private final boolean value2;
		private final double value3;
		private final long value4;
		private final int value5;
		private final String[] value6;

		public ConstructorWithMultipleParamsTestEntity(final String value1, final boolean value2, final double value3,
			final long value4, final int value5, final String[] value6) {
			super();
			this.value1 = value1;
			this.value2 = value2;
			this.value3 = value3;
			this.value4 = value4;
			this.value5 = value5;
			this.value6 = value6;
		}

	}

	@Test
	public void constructorWithMultipleParams() {
		final ConstructorWithMultipleParamsTestEntity entity = new ConstructorWithMultipleParamsTestEntity("test", true,
				3.5, 13L, 69, new String[] { "a", "b" });
		template.insert(entity);
		final ConstructorWithMultipleParamsTestEntity document = template
				.find(entity.getId(), ConstructorWithMultipleParamsTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value1, is(entity.value1));
		assertThat(document.value2, is(entity.value2));
		assertThat(document.value3, is(entity.value3));
		assertThat(document.value4, is(entity.value4));
		assertThat(document.value5, is(entity.value5));
		assertThat(document.value6, is(entity.value6));
	}

	public static class ConstructorWithRefParamsTestEntity extends BasicTestEntity {
		@Ref
		private final BasicTestEntity value1;
		@Ref
		private final Collection<BasicTestEntity> value2;

		public ConstructorWithRefParamsTestEntity(final BasicTestEntity value1,
			final Collection<BasicTestEntity> value2) {
			super();
			this.value1 = value1;
			this.value2 = value2;
		}
	}

	@Test
	public void constructorWithRefParams() {
		final BasicTestEntity value1 = new BasicTestEntity();
		final BasicTestEntity value2 = new BasicTestEntity();
		final BasicTestEntity value3 = new BasicTestEntity();
		template.insert(value1);
		template.insert(value2);
		template.insert(value3);
		final ConstructorWithRefParamsTestEntity entity = new ConstructorWithRefParamsTestEntity(value1,
				Arrays.asList(value2, value3));
		template.insert(entity);
		final ConstructorWithRefParamsTestEntity document = template
				.find(entity.id, ConstructorWithRefParamsTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value1.id, is(value1.id));
		assertThat(document.value2.size(), is(2));
		assertThat(document.value2.stream().map((e) -> e.id).collect(Collectors.toList()),
			hasItems(value2.id, value3.id));
	}

	public static class ConstructorWithRefLazyParamsTestEntity extends BasicTestEntity {
		@Ref(lazy = true)
		private final BasicTestEntity value1;
		@Ref(lazy = true)
		private final Collection<BasicTestEntity> value2;

		public ConstructorWithRefLazyParamsTestEntity(final BasicTestEntity value1,
			final Collection<BasicTestEntity> value2) {
			super();
			this.value1 = value1;
			this.value2 = value2;
		}
	}

	@Test
	public void constructorWithRefLazyParams() {
		final BasicTestEntity value1 = new BasicTestEntity();
		final BasicTestEntity value2 = new BasicTestEntity();
		final BasicTestEntity value3 = new BasicTestEntity();
		template.insert(value1);
		template.insert(value2);
		template.insert(value3);
		final ConstructorWithRefLazyParamsTestEntity entity = new ConstructorWithRefLazyParamsTestEntity(value1,
				Arrays.asList(value2, value3));
		template.insert(entity);
		final ConstructorWithRefLazyParamsTestEntity document = template
				.find(entity.id, ConstructorWithRefLazyParamsTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value1.getId(), is(value1.id));
		assertThat(document.value2.size(), is(2));
		assertThat(document.value2.stream().map((e) -> e.getId()).collect(Collectors.toList()),
			hasItems(value2.id, value3.id));
	}

	public static class ConstructorWithRelationsParamsTestEntity extends BasicTestEntity {
		@Relations(edges = BasicEdgeTestEntity.class)
		private final Collection<BasicTestEntity> value;

		public ConstructorWithRelationsParamsTestEntity(final Collection<BasicTestEntity> value) {
			super();
			this.value = value;
		}
	}

	@Test
	public void constructorWithRelationsParams() {
		final BasicTestEntity vertex1 = new BasicTestEntity();
		final BasicTestEntity vertex2 = new BasicTestEntity();
		template.insert(vertex1);
		template.insert(vertex2);
		final ConstructorWithRelationsParamsTestEntity entity = new ConstructorWithRelationsParamsTestEntity(
				Arrays.asList(vertex1, vertex2));
		template.insert(entity);
		template.insert(new BasicEdgeTestEntity(entity, vertex1));
		template.insert(new BasicEdgeTestEntity(entity, vertex2));
		final ConstructorWithRelationsParamsTestEntity document = template
				.find(entity.id, ConstructorWithRelationsParamsTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value.stream().map((e) -> e.id).collect(Collectors.toList()),
			hasItems(vertex1.id, vertex2.id));
	}

	public static class ConstructorWithRelationsLazyParamsTestEntity extends BasicTestEntity {
		@Relations(edges = BasicEdgeTestEntity.class, lazy = true)
		private final Collection<BasicTestEntity> value;

		public ConstructorWithRelationsLazyParamsTestEntity(final Collection<BasicTestEntity> value) {
			super();
			this.value = value;
		}
	}

	@Test
	public void constructorWithRelationsLazyParams() {
		final BasicTestEntity vertex1 = new BasicTestEntity();
		final BasicTestEntity vertex2 = new BasicTestEntity();
		template.insert(vertex1);
		template.insert(vertex2);
		final ConstructorWithRelationsLazyParamsTestEntity entity = new ConstructorWithRelationsLazyParamsTestEntity(
				Arrays.asList(vertex1, vertex2));
		template.insert(entity);
		template.insert(new BasicEdgeTestEntity(entity, vertex1));
		template.insert(new BasicEdgeTestEntity(entity, vertex2));
		final ConstructorWithRelationsLazyParamsTestEntity document = template
				.find(entity.id, ConstructorWithRelationsLazyParamsTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value.stream().map((e) -> e.id).collect(Collectors.toList()),
			hasItems(vertex1.id, vertex2.id));
	}

	public static class ConstructorWithFromParamsTestEntity extends BasicTestEntity {
		@From
		private final Collection<BasicEdgeLazyTestEntity> value;

		public ConstructorWithFromParamsTestEntity(final Collection<BasicEdgeLazyTestEntity> value) {
			super();
			this.value = value;
		}
	}

	@Test
	public void constructorWithFromParams() {
		final ConstructorWithFromParamsTestEntity entity = new ConstructorWithFromParamsTestEntity(null);
		template.insert(entity);
		final BasicTestEntity to = new BasicTestEntity();
		template.insert(to);
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(entity, to);
		final BasicEdgeLazyTestEntity edge2 = new BasicEdgeLazyTestEntity(entity, to);
		template.insert(edge1);
		template.insert(edge2);
		final ConstructorWithFromParamsTestEntity document = template
				.find(entity.id, ConstructorWithFromParamsTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value.stream().map((e) -> e.id).collect(Collectors.toList()), hasItems(edge1.id, edge2.id));
	}

	public static class ConstructorWithFromLazyParamsTestEntity extends BasicTestEntity {
		@From(lazy = true)
		private final Collection<BasicEdgeTestEntity> value;

		public ConstructorWithFromLazyParamsTestEntity(final Collection<BasicEdgeTestEntity> value) {
			super();
			this.value = value;
		}
	}

	@Test
	public void constructorWithFromLazyParams() {
		final ConstructorWithFromLazyParamsTestEntity entity = new ConstructorWithFromLazyParamsTestEntity(null);
		template.insert(entity);
		final BasicTestEntity to = new BasicTestEntity();
		template.insert(to);
		final BasicEdgeTestEntity edge1 = new BasicEdgeTestEntity(entity, to);
		final BasicEdgeTestEntity edge2 = new BasicEdgeTestEntity(entity, to);
		template.insert(edge1);
		template.insert(edge2);
		final ConstructorWithFromLazyParamsTestEntity document = template
				.find(entity.id, ConstructorWithFromLazyParamsTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value.stream().map((e) -> e.getId()).collect(Collectors.toList()),
			hasItems(edge1.id, edge2.id));
	}

	public static class ConstructorWithToParamsTestEntity extends BasicTestEntity {
		@To
		private final Collection<BasicEdgeLazyTestEntity> value;

		public ConstructorWithToParamsTestEntity(final Collection<BasicEdgeLazyTestEntity> value) {
			super();
			this.value = value;
		}
	}

	@Test
	public void constructorWithToParams() {
		final ConstructorWithToParamsTestEntity entity = new ConstructorWithToParamsTestEntity(null);
		template.insert(entity);
		final BasicTestEntity from = new BasicTestEntity();
		template.insert(from);
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(from, entity);
		final BasicEdgeLazyTestEntity edge2 = new BasicEdgeLazyTestEntity(from, entity);
		template.insert(edge1);
		template.insert(edge2);
		final ConstructorWithToParamsTestEntity document = template
				.find(entity.id, ConstructorWithToParamsTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value.stream().map((e) -> e.id).collect(Collectors.toList()), hasItems(edge1.id, edge2.id));
	}

	public static class ConstructorWithToLazyParamsTestEntity extends BasicTestEntity {
		@To(lazy = true)
		private final Collection<BasicEdgeTestEntity> value;

		public ConstructorWithToLazyParamsTestEntity(final Collection<BasicEdgeTestEntity> value) {
			super();
			this.value = value;
		}
	}

	@Test
	public void constructorWithToLazyParams() {
		final ConstructorWithToLazyParamsTestEntity entity = new ConstructorWithToLazyParamsTestEntity(null);
		template.insert(entity);
		final BasicTestEntity from = new BasicTestEntity();
		template.insert(from);
		final BasicEdgeTestEntity edge1 = new BasicEdgeTestEntity(from, entity);
		final BasicEdgeTestEntity edge2 = new BasicEdgeTestEntity(from, entity);
		template.insert(edge1);
		template.insert(edge2);
		final ConstructorWithToLazyParamsTestEntity document = template
				.find(entity.id, ConstructorWithToLazyParamsTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value.stream().map((e) -> e.getId()).collect(Collectors.toList()),
			hasItems(edge1.id, edge2.id));
	}

	public static class EdgeConstructorWithFromToParamsTestEntity extends BasicEdgeTestEntity {
		@From
		private final BasicTestEntity from;
		@To
		private final BasicTestEntity to;

		public EdgeConstructorWithFromToParamsTestEntity(final BasicTestEntity from, final BasicTestEntity to) {
			super();
			this.from = from;
			this.to = to;
		}
	}

	@Test
	public void edgeConstructorWithFromToParams() {
		final BasicTestEntity from = new BasicTestEntity();
		final BasicTestEntity to = new BasicTestEntity();
		template.insert(from);
		template.insert(to);
		final EdgeConstructorWithFromToParamsTestEntity edge = new EdgeConstructorWithFromToParamsTestEntity(from, to);
		template.insert(edge);
		final EdgeConstructorWithFromToParamsTestEntity document = template
				.find(edge.id, EdgeConstructorWithFromToParamsTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.from.id, is(from.id));
		assertThat(document.to.id, is(to.id));
	}

	public static class EdgeConstructorWithFromToLazyParamsTestEntity extends BasicEdgeTestEntity {
		@From
		private final BasicTestEntity from;
		@To
		private final BasicTestEntity to;

		public EdgeConstructorWithFromToLazyParamsTestEntity(final BasicTestEntity from, final BasicTestEntity to) {
			super();
			this.from = from;
			this.to = to;
		}
	}

	@Test
	public void edgeConstructorWithFromToLazyParams() {
		final BasicTestEntity from = new BasicTestEntity();
		final BasicTestEntity to = new BasicTestEntity();
		template.insert(from);
		template.insert(to);
		final EdgeConstructorWithFromToLazyParamsTestEntity edge = new EdgeConstructorWithFromToLazyParamsTestEntity(
				from, to);
		template.insert(edge);
		final EdgeConstructorWithFromToLazyParamsTestEntity document = template
				.find(edge.id, EdgeConstructorWithFromToLazyParamsTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.from.getId(), is(from.id));
		assertThat(document.to.getId(), is(to.id));
	}

	public static class JodaTestEntity extends BasicTestEntity {
		private org.joda.time.DateTime value1;
		private org.joda.time.Instant value2;
		private org.joda.time.LocalDate value3;
		private org.joda.time.LocalDateTime value4;
	}

	@Test
	public void jodaMapping() {
		final JodaTestEntity entity = new JodaTestEntity();
		entity.value1 = org.joda.time.DateTime.now();
		entity.value2 = org.joda.time.Instant.now();
		entity.value3 = org.joda.time.LocalDate.now();
		entity.value4 = org.joda.time.LocalDateTime.now();
		template.insert(entity);
		final JodaTestEntity document = template.find(entity.getId(), JodaTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value1, is(entity.value1));
		assertThat(document.value2, is(entity.value2));
		assertThat(document.value3, is(entity.value3));
		assertThat(document.value4, is(entity.value4));
	}

	public static class Java8TimeTestEntity extends BasicTestEntity {
		private java.time.Instant value1;
		private java.time.LocalDate value2;
		private java.time.LocalDateTime value3;
	}

	@Test
	public void timeMapping() {
		final Java8TimeTestEntity entity = new Java8TimeTestEntity();
		entity.value1 = java.time.Instant.now();
		entity.value2 = java.time.LocalDate.now();
		entity.value3 = java.time.LocalDateTime.now();
		template.insert(entity);
		final Java8TimeTestEntity document = template.find(entity.getId(), Java8TimeTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value1, is(entity.value1));
		assertThat(document.value2, is(entity.value2));
		assertThat(document.value3, is(entity.value3));
	}

	public static class SimpleBasicChildTestEntity extends BasicTestEntity {
		private String field;
	}

	public static class ComplexBasicChildTestEntity extends BasicTestEntity {
		private BasicTestEntity nestedEntity;
	}

	public static class PropertyInheritanceTestEntity extends BasicTestEntity {
		private BasicTestEntity value;
	}

	@Test
	public void simplePropertyInheritanceMapping() {
		final SimpleBasicChildTestEntity child = new SimpleBasicChildTestEntity();
		child.field = "value";
		final PropertyInheritanceTestEntity entity = new PropertyInheritanceTestEntity();
		entity.value = child;
		template.insert(entity);
		final PropertyInheritanceTestEntity document = template
				.find(entity.getId(), PropertyInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(SimpleBasicChildTestEntity.class)));
		assertThat(((SimpleBasicChildTestEntity) document.value).field, is(child.field));
	}

	@Test
	public void complexPropertyInheritanceMapping() {
		final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
		innerChild.field = "value";
		final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
		child.nestedEntity = innerChild;
		final PropertyInheritanceTestEntity entity = new PropertyInheritanceTestEntity();
		entity.value = child;
		template.insert(entity);
		final PropertyInheritanceTestEntity document = template
				.find(entity.getId(), PropertyInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(ComplexBasicChildTestEntity.class)));
		ComplexBasicChildTestEntity complexDocument = (ComplexBasicChildTestEntity) document.value;
		assertThat(complexDocument.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
		SimpleBasicChildTestEntity simpleDocument = (SimpleBasicChildTestEntity) complexDocument.nestedEntity;
		assertThat(simpleDocument.field, is(innerChild.field));
	}

	public static class ListInheritanceTestEntity extends BasicTestEntity {
		private List<BasicTestEntity> value;
	}

	@Test
	public void simpleListInheritanceMapping() {
		final List<BasicTestEntity> list = new ArrayList<>();
		final String value = "value";
		for (int i = 0; i < 3; ++i) {
			final SimpleBasicChildTestEntity child = new SimpleBasicChildTestEntity();
			child.field = value;
			list.add(child);
		}
		final ListInheritanceTestEntity entity = new ListInheritanceTestEntity();
		entity.value = list;
		template.insert(entity);
		final ListInheritanceTestEntity document = template.find(entity.getId(), ListInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(List.class)));
		for (BasicTestEntity elem : document.value) {
			assertThat(elem, is(instanceOf(SimpleBasicChildTestEntity.class)));
			assertThat(((SimpleBasicChildTestEntity) elem).field, is(value));
		}
	}

	@Test
	public void complexListInheritanceMapping() {
		final List<BasicTestEntity> list = new ArrayList<>();
		final String value = "value";
		for (int i = 0; i < 3; ++i) {
			final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
			innerChild.field = value;
			final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
			child.nestedEntity = innerChild;
			list.add(child);
		}
		final ListInheritanceTestEntity entity = new ListInheritanceTestEntity();
		entity.value = list;
		template.insert(entity);
		final ListInheritanceTestEntity document = template.find(entity.getId(), ListInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(List.class)));
		for (BasicTestEntity elem : document.value) {
			assertThat(elem, is(instanceOf(ComplexBasicChildTestEntity.class)));
			ComplexBasicChildTestEntity complexElem = (ComplexBasicChildTestEntity) elem;
			assertThat(complexElem.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
			SimpleBasicChildTestEntity simpleElem = (SimpleBasicChildTestEntity) complexElem.nestedEntity;
			assertThat(simpleElem.field, is(value));
		}
	}

	@SuppressWarnings("rawtypes")
	public static class UntypedListInheritanceTestEntity extends BasicTestEntity {
		private List value;
	}

	@Test
	public void untypedListInheritanceMapping() {
		final List<BasicTestEntity> list = new ArrayList<>();
		final String value = "value";
		for (int i = 0; i < 3; ++i) {
			final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
			innerChild.field = value;
			final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
			child.nestedEntity = innerChild;
			list.add(child);
		}
		final UntypedListInheritanceTestEntity entity = new UntypedListInheritanceTestEntity();
		entity.value = list;
		template.insert(entity);
		final UntypedListInheritanceTestEntity document = template
				.find(entity.getId(), UntypedListInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(List.class)));
		for (Object elem : document.value) {
			assertThat(elem, is(instanceOf(ComplexBasicChildTestEntity.class)));
			ComplexBasicChildTestEntity complexElem = (ComplexBasicChildTestEntity) elem;
			assertThat(complexElem.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
			SimpleBasicChildTestEntity simpleElem = (SimpleBasicChildTestEntity) complexElem.nestedEntity;
			assertThat(simpleElem.field, is(value));
		}
	}

	public static class MapInheritanceTestEntity extends BasicTestEntity {
		private Map<String, BasicTestEntity> value;
	}

	@Test
	public void simpleMapInheritanceMapping() {
		final Map<String, BasicTestEntity> map = new HashMap<>();
		final String value = "value";
		for (int i = 0; i < 3; ++i) {
			final SimpleBasicChildTestEntity child = new SimpleBasicChildTestEntity();
			child.field = value;
			map.put(String.valueOf(i), child);
		}
		final MapInheritanceTestEntity entity = new MapInheritanceTestEntity();
		entity.value = map;
		template.insert(entity);
		final MapInheritanceTestEntity document = template.find(entity.getId(), MapInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(Map.class)));
		for (Map.Entry<String, BasicTestEntity> entry : document.value.entrySet()) {
			assertThat(entry.getValue(), is(instanceOf(SimpleBasicChildTestEntity.class)));
			assertThat(((SimpleBasicChildTestEntity) entry.getValue()).field, is(value));
		}
	}

	@Test
	public void complexMapInheritanceMapping() {
		final Map<String, BasicTestEntity> map = new HashMap<>();
		final String value = "value";
		for (int i = 0; i < 3; ++i) {
			final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
			innerChild.field = value;
			final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
			child.nestedEntity = innerChild;
			map.put(String.valueOf(i), child);
		}
		final MapInheritanceTestEntity entity = new MapInheritanceTestEntity();
		entity.value = map;
		template.insert(entity);
		final MapInheritanceTestEntity document = template.find(entity.getId(), MapInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(Map.class)));
		for (Map.Entry<String, BasicTestEntity> entry : document.value.entrySet()) {
			assertThat(entry.getValue(), is(instanceOf(ComplexBasicChildTestEntity.class)));
			ComplexBasicChildTestEntity complexElem = (ComplexBasicChildTestEntity) entry.getValue();
			assertThat(complexElem.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
			SimpleBasicChildTestEntity simpleElem = (SimpleBasicChildTestEntity) complexElem.nestedEntity;
			assertThat(simpleElem.field, is(value));
		}
	}

	@SuppressWarnings("rawtypes")
	public static class UntypedMapInheritanceTestEntity extends BasicTestEntity {
		private Map value;
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void untypedMapInheritanceMapping() {
		final Map<String, BasicTestEntity> map = new HashMap<>();
		final String value = "value";
		for (int i = 0; i < 3; ++i) {
			final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
			innerChild.field = value;
			final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
			child.nestedEntity = innerChild;
			map.put(String.valueOf(i), child);
		}
		final UntypedMapInheritanceTestEntity entity = new UntypedMapInheritanceTestEntity();
		entity.value = map;
		template.insert(entity);
		final UntypedMapInheritanceTestEntity document = template
				.find(entity.getId(), UntypedMapInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(Map.class)));
		for (Object entry : document.value.entrySet()) {
			final Object val = ((Map.Entry) entry).getValue();
			assertThat(val, is(instanceOf(ComplexBasicChildTestEntity.class)));
			ComplexBasicChildTestEntity complexElem = (ComplexBasicChildTestEntity) val;
			assertThat(complexElem.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
			SimpleBasicChildTestEntity simpleElem = (SimpleBasicChildTestEntity) complexElem.nestedEntity;
			assertThat(simpleElem.field, is(value));
		}
	}

	public static class ConstructorWithPropertyInheritanceTestEntity extends BasicTestEntity {
		private final BasicTestEntity value;

		public ConstructorWithPropertyInheritanceTestEntity(final BasicTestEntity value) {
			this.value = value;
		}
	}

	@Test
	public void constructorPropertyInheritanceMapping() {
		final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
		innerChild.field = "value";
		final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
		child.nestedEntity = innerChild;
		final ConstructorWithPropertyInheritanceTestEntity entity = new ConstructorWithPropertyInheritanceTestEntity(
				child);
		template.insert(entity);
		final ConstructorWithPropertyInheritanceTestEntity document = template
				.find(entity.getId(), ConstructorWithPropertyInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(ComplexBasicChildTestEntity.class)));
		ComplexBasicChildTestEntity complexDocument = (ComplexBasicChildTestEntity) document.value;
		assertThat(complexDocument.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
		SimpleBasicChildTestEntity simpleDocument = (SimpleBasicChildTestEntity) complexDocument.nestedEntity;
		assertThat(simpleDocument.field, is(innerChild.field));
	}

	public static class ListInMapInheritanceTestEntity extends BasicTestEntity {
		private Map<String, List<BasicTestEntity>> value;
	}

	@Test
	public void listInMapInheritanceMapping() {
		final Map<String, List<BasicTestEntity>> map = new HashMap<>();
		final String value = "value";
		for (int i = 0; i < 3; ++i) {
			List<BasicTestEntity> list = new ArrayList<>();
			map.put(String.valueOf(i), list);
			for (int j = 0; j < 3; ++j) {
				final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
				innerChild.field = value;
				final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
				child.nestedEntity = innerChild;
				list.add(child);
			}
		}
		final ListInMapInheritanceTestEntity entity = new ListInMapInheritanceTestEntity();
		entity.value = map;
		template.insert(entity);
		final ListInMapInheritanceTestEntity document = template
				.find(entity.getId(), ListInMapInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(Map.class)));
		for (Map.Entry<String, List<BasicTestEntity>> entry : document.value.entrySet()) {
			assertThat(entry.getValue(), is(instanceOf(List.class)));
			for (BasicTestEntity elem : entry.getValue()) {
				assertThat(elem, is(instanceOf(ComplexBasicChildTestEntity.class)));
				ComplexBasicChildTestEntity complexElem = (ComplexBasicChildTestEntity) elem;
				assertThat(complexElem.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
				SimpleBasicChildTestEntity simpleElem = (SimpleBasicChildTestEntity) complexElem.nestedEntity;
				assertThat(simpleElem.field, is(value));
			}
		}
	}

	public static class PropertyRefInheritanceTestEntity extends BasicTestEntity {
		@Ref
		private BasicTestEntity value;
	}

	@Test
	public void propertyRefInheritanceMapping() {
		final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
		innerChild.field = "value";
		final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
		child.nestedEntity = innerChild;
		final PropertyRefInheritanceTestEntity entity = new PropertyRefInheritanceTestEntity();
		entity.value = child;
		template.insert(child);
		template.insert(entity);
		final PropertyRefInheritanceTestEntity document = template
				.find(entity.getId(), PropertyRefInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(ComplexBasicChildTestEntity.class)));
		ComplexBasicChildTestEntity complexDocument = (ComplexBasicChildTestEntity) document.value;
		assertThat(complexDocument.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
		SimpleBasicChildTestEntity simpleDocument = (SimpleBasicChildTestEntity) complexDocument.nestedEntity;
		assertThat(simpleDocument.field, is(innerChild.field));
	}

}
