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

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.testdata.Customer;
import com.arangodb.springframework.testdata.Knows;
import com.arangodb.springframework.testdata.Owns;
import com.arangodb.springframework.testdata.Person;
import com.arangodb.springframework.testdata.Product;
import com.arangodb.springframework.testdata.ShoppingCart;

/**
 * @author Mark Vollmary
 *
 */
public class AbstractArangoTest {

	protected static final Class<?>[] COLLECTIONS = new Class<?>[] { Customer.class, ShoppingCart.class, Product.class,
			Person.class, Owns.class, Knows.class };

	@Autowired
	protected ArangoOperations template;

	@Before
	public void before() {
		try {
			for (final Class<?> collection : COLLECTIONS) {
				template.collection(collection).drop();
			}
		} catch (final Exception e) {
		}
	}

	@After
	public void after() {
		template.dropDatabase();
	}

}
