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

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.core.convert.resolver.LazyLoadingProxy;
import com.arangodb.springframework.core.mapping.testdata.BasicTestEntity;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class LazyLoadingProxyTest extends AbstractArangoTest {

	public static class SingleReferenceLazyTestEntity extends BasicTestEntity {
		@Ref(lazy = true)
		private BasicTestEntity entity;
	}

	@Test
	public void isResolved() {
		final BasicTestEntity e1 = new BasicTestEntity();
		template.insert(e1);
		final SingleReferenceLazyTestEntity e0 = new SingleReferenceLazyTestEntity();
		e0.entity = e1;
		template.insert(e0);
		final SingleReferenceLazyTestEntity document = template.find(e0.id, SingleReferenceLazyTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.entity, is(notNullValue()));
		assertThat(document.entity, instanceOf(LazyLoadingProxy.class));
		assertThat(document.entity, instanceOf(BasicTestEntity.class));
		assertThat(((LazyLoadingProxy) document.entity).isResolved(), is(false));
		assertThat(document.entity.getId(), is(e1.getId()));
		assertThat(((LazyLoadingProxy) document.entity).isResolved(), is(true));
	}

}
