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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.testdata.Customer;

/**
 * 
 * @author Christian Lechner
 */
public class SaveEventTest extends AbstractArangoTest {

	@Autowired
	private CustomerEventListener listener;

	private Customer john;
	private Customer bob;
	private List<Customer> customers;

	public SaveEventTest() {
		super(Customer.class);
	}

	@BeforeEach
	public void createMockCustomers() {
		john = new Customer("John", "Smith", 20);
		bob = new Customer("Bob", "Thompson", 40);
		customers = new ArrayList<>();
		customers.add(john);
		customers.add(bob);
	}

	@BeforeEach
	public void clearEvents() {
		listener.beforeSaveEvents.clear();
		listener.afterSaveEvents.clear();
	}

	@Test
	public void insertSingleSaveEvent() {
		template.insert(john);
		assertThat(listener.beforeSaveEvents.size(), is(1));
		assertThat(listener.beforeSaveEvents.get(0).getSource(), is(john));

		assertThat(listener.afterSaveEvents.size(), is(1));
		assertThat(listener.afterSaveEvents.get(0).getSource(), is(john));
	}

	@Test
	public void insertMultiSaveEvent() {
		template.insert(john);
		template.insert(customers, Customer.class);

		assertThat(listener.beforeSaveEvents.size(), is(3));
		for (final BeforeSaveEvent<Customer> event : listener.beforeSaveEvents) {
			assertThat(event.getSource(), is(in(customers)));
		}

		assertThat(listener.afterSaveEvents.size(), is(2));
		for (final AfterSaveEvent<Customer> event : listener.afterSaveEvents) {
			assertThat(event.getSource(), is(in(customers)));
		}
	}

	@Test
	public void updateSingleSaveEvent() {
		template.insert(john);
		listener.beforeSaveEvents.clear();
		listener.afterSaveEvents.clear();
		john.setAge(30);
		template.update(john.getId(), john);

		assertThat(listener.beforeSaveEvents.size(), is(1));
		assertThat(listener.beforeSaveEvents.get(0).getSource(), is(john));

		assertThat(listener.afterSaveEvents.size(), is(1));
		assertThat(listener.afterSaveEvents.get(0).getSource(), is(john));
	}

	@Test
	public void updateMultiSaveEvent() {
		template.insert(customers, Customer.class);
		listener.beforeSaveEvents.clear();
		listener.afterSaveEvents.clear();
		john.setId("non-existing-id");
		bob.setAge(30);
		template.update(customers, Customer.class);

		assertThat(listener.beforeSaveEvents.size(), is(2));
		for (final BeforeSaveEvent<Customer> event : listener.beforeSaveEvents) {
			assertThat(event.getSource(), is(in(customers)));
		}

		assertThat(listener.afterSaveEvents.size(), is(1));
		assertThat(listener.afterSaveEvents.get(0).getSource(), is(bob));
	}

	@Test
	public void replaceSingleSaveEvent() {
		template.insert(john);
		listener.beforeSaveEvents.clear();
		listener.afterSaveEvents.clear();
		john.setAge(30);
		template.replace(john.getId(), john);

		assertThat(listener.beforeSaveEvents.size(), is(1));
		assertThat(listener.beforeSaveEvents.get(0).getSource(), is(john));

		assertThat(listener.afterSaveEvents.size(), is(1));
		assertThat(listener.afterSaveEvents.get(0).getSource(), is(john));
	}

	@Test
	public void replaceMultiSaveEvent() {
		template.insert(customers, Customer.class);
		listener.beforeSaveEvents.clear();
		listener.afterSaveEvents.clear();
		john.setId("non-existing-id");
		bob.setAge(30);
		template.replace(customers, Customer.class);

		assertThat(listener.beforeSaveEvents.size(), is(2));
		for (final BeforeSaveEvent<Customer> event : listener.beforeSaveEvents) {
			assertThat(event.getSource(), is(in(customers)));
		}

		assertThat(listener.afterSaveEvents.size(), is(1));
		assertThat(listener.afterSaveEvents.get(0).getSource(), is(bob));
	}

}
