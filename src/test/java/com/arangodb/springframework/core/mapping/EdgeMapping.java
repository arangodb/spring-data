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

import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;
import com.arangodb.springframework.core.mapping.testdata.BasicEdgeLazyTestEntity;
import com.arangodb.springframework.core.mapping.testdata.BasicEdgeTestEntity;
import com.arangodb.springframework.core.mapping.testdata.BasicTestEntity;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Mark Vollmary
 *
 */
abstract class EdgeMapping extends AbstractMappingTxTestAbstract {

	EdgeMapping(boolean withinTx) {
		super(withinTx, EdgeMapping.class.getDeclaredClasses());
	}

	@Test
	public void edgeFromTo() {
		final BasicTestEntity e1 = new BasicTestEntity();
		template.insert(e1, insertOpts);
		final BasicTestEntity e2 = new BasicTestEntity();
		template.insert(e2, insertOpts);
		final BasicEdgeTestEntity e0 = new BasicEdgeTestEntity(e1, e2);
		template.insert(e0, insertOpts);
		final BasicEdgeTestEntity document = template.find(e0.id, BasicEdgeTestEntity.class, findOpts).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.getFrom(), is(notNullValue()));
		assertThat(document.getFrom().getId(), is(e1.getId()));
		assertThat(document.getTo(), is(notNullValue()));
		assertThat(document.getTo().getId(), is(e2.getId()));
	}

	@Test
	public void edgeFromToLazy() {
		final BasicTestEntity e1 = new BasicTestEntity();
		template.insert(e1, insertOpts);
		final BasicTestEntity e2 = new BasicTestEntity();
		template.insert(e2, insertOpts);
		final BasicEdgeLazyTestEntity e0 = new BasicEdgeLazyTestEntity(e1, e2);
		template.insert(e0, insertOpts);
		final BasicEdgeLazyTestEntity document = template.find(e0.id, BasicEdgeLazyTestEntity.class, findOpts).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.getFrom(), is(notNullValue()));
		assertThat(document.getFrom().getId(), is(e1.getId()));
		assertThat(document.getTo(), is(notNullValue()));
		assertThat(document.getTo().getId(), is(e2.getId()));
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
		template.insert(from, insertOpts);
		template.insert(to, insertOpts);
		final EdgeConstructorWithFromToParamsTestEntity edge = new EdgeConstructorWithFromToParamsTestEntity(from, to);
		template.insert(edge, insertOpts);
		final EdgeConstructorWithFromToParamsTestEntity document = template
				.find(edge.id, EdgeConstructorWithFromToParamsTestEntity.class, findOpts).get();
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
		template.insert(from, insertOpts);
		template.insert(to, insertOpts);
		final EdgeConstructorWithFromToLazyParamsTestEntity edge = new EdgeConstructorWithFromToLazyParamsTestEntity(
				from, to);
		template.insert(edge, insertOpts);
		final EdgeConstructorWithFromToLazyParamsTestEntity document = template
				.find(edge.id, EdgeConstructorWithFromToLazyParamsTestEntity.class, findOpts).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.from.getId(), is(from.id));
		assertThat(document.to.getId(), is(to.id));
	}

}
