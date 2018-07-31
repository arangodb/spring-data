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

/**
 * 
 * @author Christian Lechner
 */
public class DeleteEventTest extends AbstractArangoTest {

	@Autowired
	private CustomerEventListener listener;

	private Customer john;
	private Customer bob;
	private List<Customer> customers;

	public DeleteEventTest() {
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
		listener.beforeDeleteEvents.clear();
		listener.afterDeleteEvents.clear();
	}

	@Test
	public void deleteSingleDeleteEvent() {
		template.insert(john);
		template.delete(john.getId(), Customer.class);

		assertThat(listener.beforeDeleteEvents.size(), is(1));
		assertThat(listener.beforeDeleteEvents.get(0).getSource(), is(john.getId()));

		assertThat(listener.afterDeleteEvents.size(), is(1));
		assertThat(listener.afterDeleteEvents.get(0).getSource(), is(john.getId()));
	}

	@Test
	public void deleteMultiDeleteEvent() {
		template.insert(customers, Customer.class);
		final List<Object> ids = customers.stream().map(c -> c.getId()).collect(Collectors.toList());
		ids.set(0, "non-existing-id");

		template.delete(ids, Customer.class);

		assertThat(listener.beforeDeleteEvents.size(), is(2));
		for (final BeforeDeleteEvent<Customer> event : listener.beforeDeleteEvents) {
			assertThat(event.getSource(), isIn(ids));
		}

		assertThat(listener.afterDeleteEvents.size(), is(1));
		assertThat(listener.afterDeleteEvents.get(0).getSource(), is(bob.getId()));
	}

}
