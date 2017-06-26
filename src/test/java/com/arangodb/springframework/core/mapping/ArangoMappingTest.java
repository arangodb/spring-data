/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
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

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.springframework.ArangoTestConfiguration;

/**
 * @author Mark - mark at arangodb.com
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class ArangoMappingTest {

	// @Autowired
	// private ApplicationContext context;
	//
	// @Test
	// public void bla() {
	// final ArangoMappingContext mappingContext = context.getBean(ArangoMappingContext.class);
	// final Optional<ArangoPersistentEntityImpl<?>> entity = mappingContext.getPersistentEntity(Customer.class);
	// final Optional<ArangoPersistentProperty> property = entity.get().getPersistentProperty("name");
	// assertThat(property.isPresent(), is(true));
	// }

}
