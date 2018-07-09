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
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.core.mapping.testdata.BasicEdgeLazyTestEntity;
import com.arangodb.springframework.core.mapping.testdata.BasicEdgeTestEntity;
import com.arangodb.springframework.core.mapping.testdata.BasicTestEntity;

/**
 * @author Mark Vollmary
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class FromMappingTest extends AbstractArangoTest {

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

	public static class DocumentFromLazySetTestEntity extends BasicTestEntity {
		@From(lazy = true)
		private Set<BasicEdgeLazyTestEntity> entities;
	}

	@Test
	public void documentFromLazySet() {
		final DocumentFromLazySetTestEntity e0 = new DocumentFromLazySetTestEntity();
		template.insert(e0);
		final DocumentFromLazySetTestEntity e1 = new DocumentFromLazySetTestEntity();
		template.insert(e1);
		final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e0, e1);
		template.insert(edge0);
		final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e0, e1);
		template.insert(edge1);
		final DocumentFromLazySetTestEntity document = template.find(e0.id, DocumentFromLazySetTestEntity.class).get();
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
}
