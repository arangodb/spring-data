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

package com.arangodb.springframework.core.template.java;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.mapping.Customer;

/**
 * @author Mark - mark at arangodb.com
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

	@Test
	public void insertDocument() {
		try {
			template.driver().db().collection("customer").drop();
		} catch (final Exception e) {
		}
		try {
			template.driver().db().createCollection("customer");
			final Customer value = new Customer();
			value.setName("John");
			final DocumentCreateEntity<Customer> res = template.insertDocument(value);
			assertThat(res, is(notNullValue()));
			assertThat(res.getId(), is(notNullValue()));
		} finally {
			template.driver().db().collection("customer").drop();
		}
	}

}
