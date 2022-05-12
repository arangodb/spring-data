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

package com.arangodb.springframework.core.mapping.event;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.testdata.Customer;
import com.arangodb.util.MapBuilder;

/**
 * 
 * @author Christian Lechner
 */
public class AfterLoadEventTest extends AbstractArangoTest {

	@Autowired
	private CustomerEventListener listener;

	private Customer john;
	private Customer bob;
	private List<Customer> customers;

	public AfterLoadEventTest() {
		super(Customer.class);
	}

	@Before
	public void createMockCustomers() {
		john = new Customer("John", "Smith", 20);
		bob = new Customer("Bob", "Thompson", 40);
		customers = new ArrayList<>();
		customers.add(john);
		customers.add(bob);
	}

	@Before
	public void clearEvents() {
		listener.afterLoadEvents.clear();
	}

	@Test
	public void findSingleAfterLoadEvent() {
		template.insert(john);
		template.find(john.getId(), Customer.class);
		assertThat(listener.afterLoadEvents.size(), is(1));
		assertThat(listener.afterLoadEvents.get(0).getSource(), is(john));
	}

	@Test
	public void findMultiAfterLoadEvent() {
		template.insert(customers, Customer.class);
		template.find(customers.stream().map(elem -> elem.getId()).collect(Collectors.toList()), Customer.class);
		assertThat(listener.afterLoadEvents.size(), is(2));
		for (final AfterLoadEvent<Customer> event : listener.afterLoadEvents) {
			assertThat(event.getSource(), isIn(customers));
		}
	}

	@Test
	public void findAllAfterLoadEvent() {
		template.insert(customers, Customer.class);
		template.findAll(Customer.class).forEach(elem -> { // trigger conversion
		});
		assertThat(listener.afterLoadEvents.size(), is(2));
		for (final AfterLoadEvent<Customer> event : listener.afterLoadEvents) {
			assertThat(event.getSource(), isIn(customers));
		}
	}

	@Test
	public void findByQueryAfterLoadEvent() {
		template.insert(customers, Customer.class);
		template.query("FOR c IN @@collection FILTER c.`customer-name` == @name RETURN c",
			new MapBuilder().put("@collection", Customer.class).put("name", john.getName()).get(), Customer.class)
				.forEach(elem -> { // trigger conversion
				});
		assertThat(listener.afterLoadEvents.size(), is(1));
		for (final AfterLoadEvent<Customer> event : listener.afterLoadEvents) {
			assertThat(event.getSource(), is(john));
		}
	}

}
