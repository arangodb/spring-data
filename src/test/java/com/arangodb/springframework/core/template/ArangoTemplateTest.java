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

package com.arangodb.springframework.core.template;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.arangodb.entity.*;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import com.arangodb.ArangoCursor;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.testdata.Address;
import com.arangodb.springframework.testdata.Customer;
import com.arangodb.springframework.testdata.Product;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoTemplateTest extends AbstractArangoTest {

	public ArangoTemplateTest() {
		super(Customer.class, Address.class, Product.class);
	}

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
		final DocumentEntity res = template.insert(new Customer("John", "Doe", 30));
		assertThat(res, is(notNullValue()));
		assertThat(res.getId(), is(notNullValue()));
	}

	@Test
	public void insertDocumentReturnNew() {
		Customer doc = new Customer("John", "Doe", 30);
		final DocumentCreateEntity<Customer> res = template.insert(doc, new DocumentCreateOptions().returnNew(true));
		assertThat(res, is(notNullValue()));
		assertThat(res.getId(), is(notNullValue()));
		Customer read = res.getNew();
		assertThat(read.getName(), is(doc.getName()));
		assertThat(read.getSurname(), is(doc.getSurname()));
		assertThat(read.getAge(), is(doc.getAge()));
	}

	@Test
	public void insertDocuments() {
		final Customer c1 = new Customer();
		final Customer c2 = new Customer();
		final Customer c3 = new Customer();
		c3.setId("3");
		final Customer c4 = new Customer();
		c4.setId("3");
		final MultiDocumentEntity<? extends DocumentEntity> res = template.insert(Arrays.asList(c1, c2, c3, c4),
				Customer.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(3));
		assertThat(res.getErrors().size(), is(1));
		assertThat(c1.getId(), is(notNullValue()));
		assertThat(c2.getId(), is(notNullValue()));
		assertThat(c3.getId(), is(notNullValue()));
	}

	@Test
	public void insertDocumentsReturnNew() {
		final Customer c1 = new Customer("John", "Doe", 30);
		final Customer c2 = new Customer();
		final Customer c3 = new Customer();
		c3.setId("3");
		final Customer c4 = new Customer();
		c4.setId("3");
		final MultiDocumentEntity<DocumentCreateEntity<Customer>> res = template.insert(
				Arrays.asList(c1, c2, c3, c4),
				new DocumentCreateOptions().returnNew(true),
				Customer.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(3));
		assertThat(res.getErrors().size(), is(1));
		assertThat(c1.getId(), is(notNullValue()));
		assertThat(c2.getId(), is(notNullValue()));
		assertThat(c3.getId(), is(notNullValue()));
		Customer read = res.getDocuments().iterator().next().getNew();
		assertThat(read.getName(), is(c1.getName()));
		assertThat(read.getSurname(), is(c1.getSurname()));
		assertThat(read.getAge(), is(c1.getAge()));
	}

	@Test
	public void repsertDocument() {
		String id = "id-" + UUID.randomUUID();
		Customer customer = new Customer("John", "Doe", 30);
		customer.setId(id);
		template.repsert(customer);
		template.repsert(customer);
		Customer read = template.find(id, Customer.class).orElseThrow();
		assertThat(read.getId(), is(customer.getId()));
		assertThat(read.getName(), is(customer.getName()));
		assertThat(read.getSurname(), is(customer.getSurname()));
		assertThat(read.getAge(), is(customer.getAge()));
	}

    @Test
    public void repsertDocumentRevConflict() {
        String id = "id-" + UUID.randomUUID();
        Customer customer = new Customer("John", "Doe", 30);
        customer.setId(id);
        template.repsert(customer);
        customer.setRev("foo");
        assertThrows(OptimisticLockingFailureException.class, () -> template.repsert(customer));
    }

	@Test
	public void insertDocumentWithCollName() {
		final DocumentEntity res = template.insert("test-customer", new Customer("John", "Doe", 30));
		assertThat(res, is(notNullValue()));
		assertThat(res.getId(), is(notNullValue()));
	}

	@Test
	public void getDocument() {
		final DocumentEntity res = template.insert(new Customer("John", "Doe", 30, new Address("22162–1010")));
		final Customer customer = template.find(res.getId(), Customer.class).get();
		assertThat(customer, is(notNullValue()));
		assertThat(customer.getName(), is("John"));
		assertThat(customer.getSurname(), is("Doe"));
		assertThat(customer.getAge(), is(30));
		assertThat(customer.getAddress(), is(notNullValue()));
		assertThat(customer.getAddress().getZipCode(), is("22162–1010"));
	}


	@Test
	public void deleteUnknownDocument() {
		assertThrows(DataRetrievalFailureException.class,
				() -> template.delete("9999", Customer.class));
	}

	@Test
	public void getDocuments() {
		final Customer c1 = new Customer("John", "Doe", 30);
		final Customer c2 = new Customer("John2", "Doe", 30);
		template.insert(Arrays.asList(c1, c2), Customer.class);
		final Iterable<Customer> customers = template.find(Arrays.asList(c1.getId(), c2.getId()), Customer.class);
		assertThat(customers, is(notNullValue()));
		assertThat(
			StreamSupport.stream(customers.spliterator(), false).map((e) -> e.getId()).collect(Collectors.toList()),
			hasItems(c1.getId(), c2.getId()));
		for (final Customer customer : customers) {
			assertThat(customer.getArangoId(), is(notNullValue()));
		}
	}

	@Test
	public void getAllDocuments() {
		final Customer c1 = new Customer("John", "Doe", 30);
		final Customer c2 = new Customer("John2", "Doe", 30);
		template.insert(Arrays.asList(c1, c2), Customer.class);
		final Iterable<Customer> customers = template.findAll(Customer.class);
		assertThat(customers, is(notNullValue()));
		assertThat(
			StreamSupport.stream(customers.spliterator(), false).map((e) -> e.getId()).collect(Collectors.toList()),
			hasItems(c1.getId(), c2.getId()));
	}

	@Test
	public void replaceDocument() {
		final DocumentEntity res = template.insert(new Customer("John", "Doe", 30));
		final DocumentEntity replaceDocument = template.replace(res.getId(), new Customer("Jane", "Doe", 26));
		assertThat(replaceDocument, is(notNullValue()));
		final Customer customer = template.find(res.getId(), Customer.class).get();
		assertThat(customer, is(notNullValue()));
		assertThat(customer.getName(), is("Jane"));
		assertThat(customer.getSurname(), is("Doe"));
		assertThat(customer.getAge(), is(26));
	}

	@Test
	public void replaceDocuments() {
		final DocumentEntity a = template.insert(new Product("a"));
		final DocumentEntity b = template.insert(new Product("b"));

		final Product documentA = template.find(a.getId(), Product.class).get();
		documentA.setName("aa");
		final Product documentB = template.find(b.getId(), Product.class).get();
		documentB.setName("bb");

		final MultiDocumentEntity<? extends DocumentEntity> res = template.replace(Arrays.asList(documentA, documentB),
			Product.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(2));

		final Product newA = template.find(a.getId(), Product.class).get();
		assertThat(newA.getName(), is("aa"));
		final Product newB = template.find(b.getId(), Product.class).get();
		assertThat(newB.getName(), is("bb"));
	}

	@Test
	public void updateDocument() {
		final DocumentEntity res = template.insert(new Customer("John", "Doe", 30));
		template.update(res.getId(), new Customer("Jane", "Doe", 26));
		final Customer customer = template.find(res.getId(), Customer.class).get();
		assertThat(customer, is(notNullValue()));
		assertThat(customer.getName(), is("Jane"));
		assertThat(customer.getSurname(), is("Doe"));
		assertThat(customer.getAge(), is(26));
	}

    @Test
    public void updateDocumentRevConflict() {
        final DocumentEntity res = template.insert(new Customer("John", "Doe", 30));
        Customer doc = new Customer("Jane", "Doe", 26);
        doc.setRev("foo");
        assertThrows(OptimisticLockingFailureException.class, () ->
                template.update(res.getId(), doc, new DocumentUpdateOptions().ignoreRevs(false)));
    }

	@Test
	public void updateDocuments() {
		final DocumentEntity a = template.insert(new Product("a"));
		final DocumentEntity b = template.insert(new Product("b"));

		final Product documentA = template.find(a.getId(), Product.class).get();
		documentA.setName("aa");
		final Product documentB = template.find(b.getId(), Product.class).get();
		documentB.setName("bb");

		final MultiDocumentEntity<? extends DocumentEntity> res = template.update(Arrays.asList(documentA, documentB),
			Product.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(2));

		final Product newA = template.find(a.getId(), Product.class).get();
		assertThat(newA.getName(), is("aa"));
		final Product newB = template.find(b.getId(), Product.class).get();
		assertThat(newB.getName(), is("bb"));
	}

	@Test
	public void deleteDocument() {
		final DocumentEntity res = template.insert(new Customer("John", "Doe", 30));
		template.delete(res.getId(), Customer.class);
		final Optional<Customer> customer = template.find(res.getId(), Customer.class);
		assertThat(customer.isPresent(), is(false));
	}

	@Test
	public void deleteDocuments() {
		final DocumentEntity a = template.insert(new Product("a"));
		final DocumentEntity b = template.insert(new Product("b"));

		final Product documentA = template.find(a.getId(), Product.class).get();
		final Product documentB = template.find(b.getId(), Product.class).get();

		final MultiDocumentEntity<? extends DocumentEntity> res = template.delete(Arrays.asList(documentA, documentB),
			Product.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(2));

		final Optional<Product> deletedA = template.find(a.getId(), Product.class);
		assertThat(deletedA.isPresent(), is(false));
		final Optional<Product> deletedB = template.find(b.getId(), Product.class);
		assertThat(deletedB.isPresent(), is(false));
	}

	@Test
	public void query() {
		template.insert(new Customer("John", "Doe", 30));
		Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("@coll", "test-customer");
		bindVars.put("name", "John");
		final ArangoCursor<Customer> cursor = template.query("FOR c IN @@coll FILTER c.`customer-name` == @name RETURN c",
			bindVars, new AqlQueryOptions(), Customer.class);
		assertThat(cursor, is(notNullValue()));
		final List<Customer> customers = cursor.asListRemaining();
		assertThat(customers.size(), is(1));
		assertThat(customers.get(0).getName(), is("John"));
		assertThat(customers.get(0).getSurname(), is("Doe"));
		assertThat(customers.get(0).getAge(), is(30));
	}

	@Test
	public void queryWithoutBindParams() {
		template.insert(new Customer("John", "Doe", 30));
		final ArangoCursor<Customer> cursor = template.query("FOR c IN `test-customer` FILTER c.`customer-name` == 'John' RETURN c", null,
			new AqlQueryOptions(), Customer.class);
		assertThat(cursor, is(notNullValue()));
		final List<Customer> customers = cursor.asListRemaining();
		assertThat(customers.size(), is(1));
		assertThat(customers.get(0).getName(), is("John"));
		assertThat(customers.get(0).getSurname(), is("Doe"));
		assertThat(customers.get(0).getAge(), is(30));
	}

	@Test
	public void updateQueryForUnknown() {
		assertThrows(DataRetrievalFailureException.class,
				() -> template.query("UPDATE '9999' WITH { age: 99 } IN `test-customer` RETURN NEW", Customer.class));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void queryMap() {
		template.insert(new Customer("John", "Doe", 30));
		Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("@coll", "test-customer");
		bindVars.put("name", "John");

		final ArangoCursor<Map> cursor = template.query("FOR c IN @@coll FILTER c.`customer-name` == @name RETURN c",
			bindVars, new AqlQueryOptions(), Map.class);
		assertThat(cursor, is(notNullValue()));
		final List<Map> customers = cursor.asListRemaining();
		assertThat(customers.size(), is(1));
		assertThat(customers.get(0).get("customer-name"), is("John"));
		assertThat(customers.get(0).get("surname"), is("Doe"));
		assertThat(customers.get(0).get("age"), is(30));
	}

	@Test
	public void queryBaseDocument() {
		template.insert(new Customer("John", "Doe", 30));
		Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("@coll", "test-customer");
		bindVars.put("name", "John");

		final ArangoCursor<BaseDocument> cursor = template.query("FOR c IN @@coll FILTER c.`customer-name` == @name RETURN c",
			bindVars, new AqlQueryOptions(),
			BaseDocument.class);
		assertThat(cursor, is(notNullValue()));
		final List<BaseDocument> customers = cursor.asListRemaining();
		assertThat(customers.size(), is(1));
		assertThat(customers.get(0).getAttribute("customer-name"), is("John"));
		assertThat(customers.get(0).getAttribute("surname"), is("Doe"));
		assertThat(customers.get(0).getAttribute("age"), is(30));
	}

	@Test
	public void queryJsonNodeSlice() {
		template.insert(new Customer("John", "Doe", 30));
		Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("@coll", "test-customer");
		bindVars.put("name", "John");
		final ArangoCursor<ObjectNode> cursor = template.query("FOR c IN @@coll FILTER c.`customer-name` == @name RETURN c",
				bindVars, new AqlQueryOptions(),
				ObjectNode.class);
		assertThat(cursor, is(notNullValue()));
		final List<ObjectNode> customers = cursor.asListRemaining();
		assertThat(customers.size(), is(1));
		assertThat(customers.get(0).get("customer-name").textValue(), is("John"));
		assertThat(customers.get(0).get("surname").textValue(), is("Doe"));
		assertThat(customers.get(0).get("age").intValue(), is(30));
	}

	static class TransientTestEntity {
		@Id
		private String id;
		@Transient
		private String transientField;
	}

	@Test
	public void transientTest() {
		final TransientTestEntity value = new TransientTestEntity();
		value.transientField = "test";
		template.insert(value);
		final Optional<TransientTestEntity> find = template.find(value.id, TransientTestEntity.class);
		assertThat(find.isPresent(), is(true));
		assertThat(find.get().transientField, is(nullValue()));
	}

	public static class NewEntityTest implements Persistable<String> {

		@Id
		private final String id;
		@Transient
		private transient boolean persisted;

		public NewEntityTest(final String id) {
			super();
			this.id = id;
		}

		public void setPersisted(final boolean persisted) {
			this.persisted = persisted;
		}

		@Override
		public boolean isNew() {
			return !persisted;
		}

		@Override
		public String getId() {
			return id;
		}
	}

	@Document
	static class MapContentTestEntity {
		@Id
		String id;
		Map<String, Object> value;
	}

}
