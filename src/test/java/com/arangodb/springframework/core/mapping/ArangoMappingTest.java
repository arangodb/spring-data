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

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
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

	public static class SingleReferenceTestEntity extends BasicTestEntity {
		@Ref
		private BasicTestEntity entity;
	}

	@Test
	public void singleRef() {
		final BasicTestEntity e1 = new BasicTestEntity();
		e1.id = template.insertDocument(e1).getId();
		final SingleReferenceTestEntity e0 = new SingleReferenceTestEntity();
		e0.entity = e1;
		e0.id = template.insertDocument(e0).getId();
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
		e1.id = template.insertDocument(e1).getId();
		final SingleReferenceLazyTestEntity e0 = new SingleReferenceLazyTestEntity();
		e0.entity = e1;
		e0.id = template.insertDocument(e0).getId();
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
		e1.id = template.insertDocument(e1).getId();
		final BasicTestEntity e2 = new BasicTestEntity();
		e2.id = template.insertDocument(e2).getId();
		final MultiReferenceTestEntity e0 = new MultiReferenceTestEntity();
		e0.entities = Arrays.asList(e1, e2);
		e0.id = template.insertDocument(e0).getId();
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
		e1.id = template.insertDocument(e1).getId();
		final BasicTestEntity e2 = new BasicTestEntity();
		e2.id = template.insertDocument(e2).getId();
		final MultiReferenceLazyTestEntity e0 = new MultiReferenceLazyTestEntity();
		e0.entities = Arrays.asList(e1, e2);
		e0.id = template.insertDocument(e0).getId();
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
		e1.id = template.insertDocument(e1).getId();
		final BasicTestEntity e2 = new BasicTestEntity();
		e2.id = template.insertDocument(e2).getId();
		final BasicEdgeTestEntity e0 = new BasicEdgeTestEntity(e1, e2);
		e0.id = template.insertDocument(e0).getId();
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
		e1.id = template.insertDocument(e1).getId();
		final BasicTestEntity e2 = new BasicTestEntity();
		e2.id = template.insertDocument(e2).getId();
		final BasicEdgeLazyTestEntity e0 = new BasicEdgeLazyTestEntity(e1, e2);
		e0.id = template.insertDocument(e0).getId();
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
		e0.id = template.insertDocument(e0).getId();
		final DocumentFromTestEntity e1 = new DocumentFromTestEntity();
		e1.id = template.insertDocument(e1).getId();
		final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e0, e1);
		edge0.id = template.insertDocument(edge0).getId();
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e0, e1);
		edge1.id = template.insertDocument(edge1).getId();
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
	@Ignore
	public void documentFromLazy() {
		final DocumentFromLazyTestEntity e0 = new DocumentFromLazyTestEntity();
		e0.id = template.insertDocument(e0).getId();
		final DocumentFromLazyTestEntity e1 = new DocumentFromLazyTestEntity();
		e1.id = template.insertDocument(e1).getId();
		final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e0, e1);
		edge0.id = template.insertDocument(edge0).getId();
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e0, e1);
		edge1.id = template.insertDocument(edge1).getId();
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
		e0.id = template.insertDocument(e0).getId();
		final DocumentToTestEntity e1 = new DocumentToTestEntity();
		e1.id = template.insertDocument(e1).getId();
		final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e1, e0);
		edge0.id = template.insertDocument(edge0).getId();
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e1, e0);
		edge1.id = template.insertDocument(edge1).getId();
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
	@Ignore
	public void documentToLazy() {
		final DocumentToLazyTestEntity e0 = new DocumentToLazyTestEntity();
		e0.id = template.insertDocument(e0).getId();
		final DocumentToLazyTestEntity e1 = new DocumentToLazyTestEntity();
		e1.id = template.insertDocument(e1).getId();
		final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e1, e0);
		edge0.id = template.insertDocument(edge0).getId();
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e1, e0);
		edge1.id = template.insertDocument(edge1).getId();
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
		e1.id = template.insertDocument(e1).getId();
		final BasicTestEntity e2 = new BasicTestEntity();
		e2.id = template.insertDocument(e2).getId();
		final RelationsTestEntity e0 = new RelationsTestEntity();
		e0.id = template.insertDocument(e0).getId();
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
	@Ignore
	public void relationsLazy() {
		final BasicTestEntity e1 = new BasicTestEntity();
		e1.id = template.insertDocument(e1).getId();
		final BasicTestEntity e2 = new BasicTestEntity();
		e2.id = template.insertDocument(e2).getId();
		final RelationsTestEntity e0 = new RelationsTestEntity();
		e0.id = template.insertDocument(e0).getId();
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

}
