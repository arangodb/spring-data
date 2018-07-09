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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.annotation.To;
import com.arangodb.springframework.core.mapping.testdata.BasicEdgeLazyTestEntity;
import com.arangodb.springframework.core.mapping.testdata.BasicEdgeTestEntity;
import com.arangodb.springframework.core.mapping.testdata.BasicTestEntity;

/**
 * @author Mark Vollmary
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class ToMappingTest extends AbstractArangoTest {

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

	public static class DocumentToLazyTestSetEntity extends BasicTestEntity {
		@To(lazy = true)
		private Collection<BasicEdgeLazyTestEntity> entities;
	}

	@Test
	public void documentToLazySet() {
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
}
