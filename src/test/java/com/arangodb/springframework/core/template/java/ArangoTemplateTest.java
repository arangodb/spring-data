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

package com.arangodb.springframework.core.template.java;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.mapping.Customer;

/**
 * @author Mark Vollmary
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class ArangoTemplateTest {

	@Autowired
	private ArangoOperations template;

	@Test
	public void template() {
		final ArangoDBVersion version = template.getVersion();
		assertThat(version, is(notNullValue()));
		assertThat(version.getLicense(), is(notNullValue()));
		assertThat(version.getServer(), is(notNullValue()));
		assertThat(version.getVersion(), is(notNullValue()));
	}

	@Before
	public void before() {
		try {
			template.driver().db().collection("customer").drop();
		} catch (final Exception e) {
		}
		template.driver().db().createCollection("customer");
	}

	@After
	public void after() {
		try {
			template.driver().db().collection("customer").drop();
		} catch (final Exception e) {
		}
	}

	@Test
	public void insertDocument() {
		final DocumentCreateEntity<Customer> res = template.insertDocument(new Customer("John", "Doe"));
		assertThat(res, is(notNullValue()));
		assertThat(res.getId(), is(notNullValue()));
	}

	@Test
	public void getDocument() {
		final DocumentCreateEntity<Customer> res = template.insertDocument(new Customer("John", "Doe"));
		final Customer customer = template.getDocument(res.getKey(), Customer.class);
		assertThat(customer, is(notNullValue()));
		assertThat(customer.getName(), is("John"));
		assertThat(customer.getSurname(), is("Doe"));
	}

	@Test
	public void replaceDocument() {
		final DocumentCreateEntity<Customer> res = template.insertDocument(new Customer("John", "Doe"));
		final DocumentUpdateEntity<Customer> replaceDocument = template.replaceDocument(res.getKey(),
			new Customer("Jane", "Doe"));
		assertThat(replaceDocument, is(notNullValue()));
		final Customer customer = template.getDocument(res.getKey(), Customer.class);
		assertThat(customer, is(notNullValue()));
		assertThat(customer.getName(), is("Jane"));
		assertThat(customer.getSurname(), is("Doe"));
	}

	@Test
	public void updateDocument() {
		final DocumentCreateEntity<Customer> res = template.insertDocument(new Customer("John", "Doe"));
		template.updateDocument(res.getKey(), new Customer("Jane", "Doe"));
		final Customer customer = template.getDocument(res.getKey(), Customer.class);
		assertThat(customer, is(notNullValue()));
		assertThat(customer.getName(), is("Jane"));
		assertThat(customer.getSurname(), is("Doe"));
	}

	@Test
	public void deleteDocument() {
		final DocumentCreateEntity<Customer> res = template.insertDocument(new Customer("John", "Doe"));
		template.deleteDocument(res.getKey(), Customer.class);
		final Customer customer = template.getDocument(res.getKey(), Customer.class);
		assertThat(customer, is(nullValue()));
	}

}
