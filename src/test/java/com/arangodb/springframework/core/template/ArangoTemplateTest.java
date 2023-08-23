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
import com.arangodb.model.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import com.arangodb.ArangoCursor;
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
		assertThat(template.db(), is(notNullValue()));
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
		final MultiDocumentEntity<? extends DocumentEntity> res = template.insertAll(Arrays.asList(c1, c2, c3, c4),
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
		final MultiDocumentEntity<DocumentCreateEntity<Customer>> res = template.insertAll(
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
		Customer customer = new Customer("John", "Doe", 30);
		Customer repsert1 = template.repsert(customer);
		Customer repsert2 = template.repsert(customer);

		assertThat(repsert1.getId(), is(customer.getId()));
		assertThat(repsert1.getName(), is(customer.getName()));
		assertThat(repsert1.getSurname(), is(customer.getSurname()));
		assertThat(repsert1.getAge(), is(customer.getAge()));

		assertThat(repsert2.getId(), is(customer.getId()));
		assertThat(repsert2.getName(), is(customer.getName()));
		assertThat(repsert2.getSurname(), is(customer.getSurname()));
		assertThat(repsert2.getAge(), is(customer.getAge()));
	}

	@Test
	public void repsertDocuments() {
		List<Customer> customers = List.of(
				new Customer("John", "Doe", 11),
				new Customer("John2", "Doe2", 22)
		);
		Iterable<Customer> res = template.repsertAll(customers, Customer.class);
		Iterator<Customer> cIt = customers.iterator();
		for (Customer re : res) {
			Customer c = cIt.next();
			assertThat(re.getId(), is(c.getId()));
			assertThat(re.getName(), is(c.getName()));
			assertThat(re.getSurname(), is(c.getSurname()));
			assertThat(re.getAge(), is(c.getAge()));
		}
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
		template.insertAll(Arrays.asList(c1, c2), Customer.class);
		final Iterable<Customer> customers = template.findAll(Arrays.asList(c1.getId(), c2.getId()), Customer.class);
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
		template.insertAll(Arrays.asList(c1, c2), Customer.class);
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
    public void replaceDocumentReturnNew() {
        Customer doc1 = new Customer("John", "Doe", 30);
        Customer doc2 = new Customer("Jane", "Doe", 26);
        final DocumentEntity res = template.insert(doc1);
        Customer replacedDocument = template.replace(res.getId(), doc2, new DocumentReplaceOptions().returnNew(true)).getNew();
        assertThat(replacedDocument, is(notNullValue()));
        assertThat(replacedDocument.getName(), is(doc2.getName()));
        assertThat(replacedDocument.getSurname(), is(doc2.getSurname()));
        assertThat(replacedDocument.getAge(), is(doc2.getAge()));
    }

	@Test
	public void replaceDocuments() {
		final DocumentEntity a = template.insert(new Product("a"));
		final DocumentEntity b = template.insert(new Product("b"));

		final Product documentA = template.find(a.getId(), Product.class).get();
		documentA.setName("aa");
		final Product documentB = template.find(b.getId(), Product.class).get();
		documentB.setName("bb");

		final MultiDocumentEntity<? extends DocumentEntity> res = template.replaceAll(Arrays.asList(documentA, documentB),
				Product.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(2));

		final Product newA = template.find(a.getId(), Product.class).get();
		assertThat(newA.getName(), is("aa"));
		final Product newB = template.find(b.getId(), Product.class).get();
		assertThat(newB.getName(), is("bb"));
	}

	@Test
	public void replaceDocumentsReturnNew() {
		final DocumentEntity a = template.insert(new Product("a"));
		final DocumentEntity b = template.insert(new Product("b"));

		final Product documentA = template.find(a.getId(), Product.class).get();
		documentA.setName("aa");
		final Product documentB = template.find(b.getId(), Product.class).get();
		documentB.setName("bb");

        MultiDocumentEntity<DocumentUpdateEntity<Product>> res = template.replaceAll(Arrays.asList(documentA, documentB),
                new DocumentReplaceOptions().returnNew(true),
                Product.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(2));

        Iterator<DocumentUpdateEntity<Product>> it = res.getDocuments().iterator();
		final Product newA = it.next().getNew();
		assertThat(newA.getName(), is("aa"));
		final Product newB = it.next().getNew();
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
	public void updateDocumentReturnNew() {
		Customer doc = new Customer("John", "Doe", 30);
		Customer doc2 = new Customer("Jane", "Doe", 26);
		final DocumentEntity res = template.insert(doc);
		Customer customer = template.update(res.getId(), doc2, new DocumentUpdateOptions().returnNew(true)).getNew();
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

		final MultiDocumentEntity<? extends DocumentEntity> res = template.updateAll(Arrays.asList(documentA, documentB),
				Product.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(2));

		final Product newA = template.find(a.getId(), Product.class).get();
		assertThat(newA.getName(), is("aa"));
		final Product newB = template.find(b.getId(), Product.class).get();
		assertThat(newB.getName(), is("bb"));
	}

	@Test
	public void updateDocumentsReturnNew() {
		final DocumentEntity a = template.insert(new Product("a"));
		final DocumentEntity b = template.insert(new Product("b"));

		final Product documentA = template.find(a.getId(), Product.class).get();
		documentA.setName("aa");
		final Product documentB = template.find(b.getId(), Product.class).get();
		documentB.setName("bb");

		final MultiDocumentEntity<DocumentUpdateEntity<Product>> res = template.updateAll(Arrays.asList(documentA, documentB),
				new DocumentUpdateOptions().returnNew(true), Product.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(2));

		Iterator<DocumentUpdateEntity<Product>> resIt = res.getDocuments().iterator();

		final Product newA = resIt.next().getNew();
		assertThat(newA.getName(), is("aa"));
		final Product newB = resIt.next().getNew();
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
	public void deleteDocumentWithRev() {
		var doc = new Customer("John", "Doe", 30);
		DocumentEntity res = template.insert(doc);
		template.delete(doc.getId(), new DocumentDeleteOptions().ifMatch(doc.getRev()), Customer.class);
		Optional<Customer> customer = template.find(res.getId(), Customer.class);
		assertThat(customer.isPresent(), is(false));
	}

	@Test
	public void deleteDocumentWithRevConflict() {
		Customer doc = new Customer("John", "Doe", 30);
		DocumentEntity res = template.insert(doc);

		Customer doc2 = template.find(res.getId(), Customer.class).get();
		doc2.setName("Johnny");
		template.update(doc2.getId(), doc2);

		assertThrows(OptimisticLockingFailureException.class, () ->
				template.delete(doc.getId(), new DocumentDeleteOptions().ifMatch(doc.getRev()), Customer.class));
	}

	@Test
	public void deleteDocumentReturnOld() {
		Customer doc = new Customer("John", "Doe", 30);
		String id = template.insert(doc).getId();
		Customer deleted = template.delete(id, new DocumentDeleteOptions().returnOld(true), Customer.class).getOld();
		final Optional<Customer> customer = template.find(id, Customer.class);
		assertThat(customer.isPresent(), is(false));
		assertThat(deleted.getName(), is(doc.getName()));
		assertThat(deleted.getSurname(), is(doc.getSurname()));
		assertThat(deleted.getAge(), is(doc.getAge()));
	}

	@Test
	public void deleteDocuments() {
		final DocumentEntity a = template.insert(new Product("a"));
		final DocumentEntity b = template.insert(new Product("b"));

		final Product documentA = template.find(a.getId(), Product.class).get();
		final Product documentB = template.find(b.getId(), Product.class).get();

		final MultiDocumentEntity<DocumentDeleteEntity<?>> res = template.deleteAll(Arrays.asList(documentA, documentB), Product.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(2));

		final Optional<Product> deletedA = template.find(a.getId(), Product.class);
		assertThat(deletedA.isPresent(), is(false));
		final Optional<Product> deletedB = template.find(b.getId(), Product.class);
		assertThat(deletedB.isPresent(), is(false));
	}

	@Test
	public void deleteDocumentsWithRev() {
		var a = template.insert(new Product("a"), new DocumentCreateOptions().returnNew(true));
		var b = template.insert(new Product("b"),  new DocumentCreateOptions().returnNew(true));

		MultiDocumentEntity<DocumentDeleteEntity<Product>> res = template.deleteAll(List.of(a, b),
				new DocumentDeleteOptions().ignoreRevs(false),
				Product.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(2));

		final Optional<Product> deletedA = template.find(a.getId(), Product.class);
		assertThat(deletedA.isPresent(), is(false));
		final Optional<Product> deletedB = template.find(b.getId(), Product.class);
		assertThat(deletedB.isPresent(), is(false));
	}

	@Test
	public void deleteDocumentsWithRevConflict() {
		var a = template.insert(new Product("a"), new DocumentCreateOptions().returnNew(true));
		var b = template.insert(new Product("b"),  new DocumentCreateOptions().returnNew(true));

		var a2 = template.find(a.getId(), Product.class).get();
		a2.setName("aa");
		template.update(a2.getId(), a2);

		MultiDocumentEntity<DocumentDeleteEntity<Product>> res = template.deleteAll(List.of(a, b),
				new DocumentDeleteOptions().ignoreRevs(false), Product.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(1));
		assertThat(res.getDocuments().get(0).getKey(), is(b.getKey()));
		assertThat(res.getErrors().size(), is(1));
		assertThat(res.getErrors().get(0).getErrorNum(), is(1200));
		assertThat(res.getErrors().get(0).getErrorMessage(), containsString("conflict, _rev values do not match"));

		assertThat(template.find(a.getId(), Product.class).isPresent(), is(true));
		assertThat(template.find(b.getId(), Product.class).isPresent(), is(false));
	}

	@Test
	public void deleteDocumentsReturnOld() {
		final DocumentEntity a = template.insert(new Product("a"));
		final DocumentEntity b = template.insert(new Product("b"));

		final Product documentA = template.find(a.getId(), Product.class).get();
		final Product documentB = template.find(b.getId(), Product.class).get();

		final MultiDocumentEntity<DocumentDeleteEntity<Product>> res = template.deleteAll(
				Arrays.asList(documentA, documentB), new DocumentDeleteOptions().returnOld(true), Product.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(2));

		final Optional<Product> deletedA = template.find(a.getId(), Product.class);
		assertThat(deletedA.isPresent(), is(false));
		final Optional<Product> deletedB = template.find(b.getId(), Product.class);
		assertThat(deletedB.isPresent(), is(false));

		Iterator<DocumentDeleteEntity<Product>> dit = res.getDocuments().iterator();
		Product retA = dit.next().getOld();
		assertThat(retA.getName(), is("a"));
		Product retB = dit.next().getOld();
		assertThat(retB.getName(), is("b"));
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
