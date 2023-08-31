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

package com.arangodb.springframework;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.arangodb.springframework.core.ArangoOperations;

/**
 * @author Mark Vollmary
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public abstract class AbstractArangoTest {

	private static volatile ArangoOperations staticTemplate;

	@Autowired
	protected ArangoOperations template;
	protected final Class<?>[] collections;

	protected AbstractArangoTest(final Class<?>... collections) {
		super();
		this.collections = collections;
	}

	@BeforeEach
	public void before() {
		for (final Class<?> collection : collections) {
			template.collection(collection).truncate();
		}
		AbstractArangoTest.staticTemplate = template;
	}

	@AfterAll
	public static void afterClass() {
		staticTemplate.dropDatabase();
	}

}
