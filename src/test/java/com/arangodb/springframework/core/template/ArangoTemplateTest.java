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

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.annotation.Key;
import com.arangodb.springframework.core.ArangoOperations.UpsertStrategy;
import com.arangodb.springframework.testdata.Address;
import com.arangodb.springframework.testdata.Customer;
import com.arangodb.springframework.testdata.Product;
import com.arangodb.util.MapBuilder;
import com.arangodb.velocypack.VPackSlice;

/**
 * @author Mark Vollmary
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class ArangoTemplateTest extends AbstractArangoTest {

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
	public void insertDocuments() {
		final Customer c1 = new Customer();
		final Customer c2 = new Customer();
		final Customer c3 = new Customer();
		c3.setKey("3");
		final Customer c4 = new Customer();
		c4.setKey("3");
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
	public void insertDocumentWithCollName() {
		final DocumentEntity res = template.insert("customer", new Customer("John", "Doe", 30));
		assertThat(res, is(notNullValue()));
		assertThat(res.getId(), is(notNullValue()));
	}

	@Test
	public void upsertReplace() {
		final Customer customer = new Customer("John", "Doe", 30);
		template.upsert(customer, UpsertStrategy.REPLACE);
		assertThat(template.find(customer.getId(), Customer.class).get().getAge(), is(30));
		customer.setAge(35);
		template.upsert(customer, UpsertStrategy.REPLACE);
		assertThat(template.find(customer.getId(), Customer.class).get().getAge(), is(35));
	}

	@Test
	public void upsertUpdate() {
		final Customer customer = new Customer("John", "Doe", 30);
		template.upsert(customer, UpsertStrategy.UPDATE);
		assertThat(template.find(customer.getId(), Customer.class).get().getAge(), is(30));
		customer.setAge(35);
		template.upsert(customer, UpsertStrategy.UPDATE);
		assertThat(template.find(customer.getId(), Customer.class).get().getAge(), is(35));
	}

	@Test
	public void upsertReplaceMultiple() {
		final Customer c1 = new Customer("John", "Doe", 30);
		final Customer c2 = new Customer("John2", "Doe2", 30);
		template.upsert(Arrays.asList(c1, c2), UpsertStrategy.REPLACE);
		assertThat(template.find(c1.getId(), Customer.class).get().getAge(), is(30));
		assertThat(template.find(c2.getId(), Customer.class).get().getAge(), is(30));
		c1.setAge(35);
		c2.setAge(35);
		final Customer c3 = new Customer("John3", "Doe2", 30);
		template.upsert(Arrays.asList(c1, c2, c3), UpsertStrategy.REPLACE);
		assertThat(template.find(c1.getId(), Customer.class).get().getAge(), is(35));
		assertThat(template.find(c2.getId(), Customer.class).get().getAge(), is(35));
		assertThat(template.find(c3.getId(), Customer.class).get().getAge(), is(30));
	}

	@Test
	public void upsertUpdateMultiple() {
		final Customer c1 = new Customer("John", "Doe", 30);
		final Customer c2 = new Customer("John2", "Doe2", 30);
		template.upsert(Arrays.asList(c1, c2), UpsertStrategy.UPDATE);
		assertThat(template.find(c1.getId(), Customer.class).get().getAge(), is(30));
		assertThat(template.find(c2.getId(), Customer.class).get().getAge(), is(30));
		c1.setAge(35);
		c2.setAge(35);
		final Customer c3 = new Customer("John3", "Doe2", 30);
		template.upsert(Arrays.asList(c1, c2, c3), UpsertStrategy.UPDATE);
		assertThat(template.find(c1.getId(), Customer.class).get().getAge(), is(35));
		assertThat(template.find(c2.getId(), Customer.class).get().getAge(), is(35));
		assertThat(template.find(c3.getId(), Customer.class).get().getAge(), is(30));
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
	public void getDocuments() {
		final Customer c1 = new Customer("John", "Doe", 30);
		final Customer c2 = new Customer("John2", "Doe", 30);
		template.insert(Arrays.asList(c1, c2), Customer.class);
		final Iterable<Customer> customers = template.find(Arrays.asList(c1.getId(), c2.getId()), Customer.class);
		assertThat(customers, is(notNullValue()));
		assertThat(
			StreamSupport.stream(customers.spliterator(), false).map((e) -> e.getId()).collect(Collectors.toList()),
			hasItems(c1.getId(), c2.getId()));
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
		final ArangoCursor<Customer> cursor = template.query("FOR c IN @@coll FILTER c.name == @name RETURN c",
			new MapBuilder().put("@coll", "customer").put("name", "John").get(), new AqlQueryOptions(), Customer.class);
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
		final ArangoCursor<Customer> cursor = template.query("FOR c IN customer FILTER c.name == 'John' RETURN c", null,
			new AqlQueryOptions(), Customer.class);
		assertThat(cursor, is(notNullValue()));
		final List<Customer> customers = cursor.asListRemaining();
		assertThat(customers.size(), is(1));
		assertThat(customers.get(0).getName(), is("John"));
		assertThat(customers.get(0).getSurname(), is("Doe"));
		assertThat(customers.get(0).getAge(), is(30));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void queryMap() {
		template.insert(new Customer("John", "Doe", 30));
		final ArangoCursor<Map> cursor = template.query("FOR c IN @@coll FILTER c.name == @name RETURN c",
			new MapBuilder().put("@coll", "customer").put("name", "John").get(), new AqlQueryOptions(), Map.class);
		assertThat(cursor, is(notNullValue()));
		final List<Map> customers = cursor.asListRemaining();
		assertThat(customers.size(), is(1));
		assertThat(customers.get(0).get("name"), is("John"));
		assertThat(customers.get(0).get("surname"), is("Doe"));
		assertThat(customers.get(0).get("age"), is(30L));
	}

	@Test
	public void queryBaseDocument() {
		template.insert(new Customer("John", "Doe", 30));
		final ArangoCursor<BaseDocument> cursor = template.query("FOR c IN @@coll FILTER c.name == @name RETURN c",
			new MapBuilder().put("@coll", "customer").put("name", "John").get(), new AqlQueryOptions(),
			BaseDocument.class);
		assertThat(cursor, is(notNullValue()));
		final List<BaseDocument> customers = cursor.asListRemaining();
		assertThat(customers.size(), is(1));
		assertThat(customers.get(0).getAttribute("name"), is("John"));
		assertThat(customers.get(0).getAttribute("surname"), is("Doe"));
		assertThat(customers.get(0).getAttribute("age"), is(30L));
	}

	@Test
	public void queryVPackSlice() {
		template.insert(new Customer("John", "Doe", 30));
		final ArangoCursor<VPackSlice> cursor = template.query("FOR c IN @@coll FILTER c.name == @name RETURN c",
			new MapBuilder().put("@coll", "customer").put("name", "John").get(), new AqlQueryOptions(),
			VPackSlice.class);
		assertThat(cursor, is(notNullValue()));
		final List<VPackSlice> customers = cursor.asListRemaining();
		assertThat(customers.size(), is(1));
		assertThat(customers.get(0).get("name").getAsString(), is("John"));
		assertThat(customers.get(0).get("surname").getAsString(), is("Doe"));
		assertThat(customers.get(0).get("age").getAsInt(), is(30));
	}

	public static class NewEntityTest implements Persistable<String> {

		@Id
		private String id;
		@Key
		private final String key;
		@Transient
		private transient boolean persisted;

		public NewEntityTest(final String key) {
			super();
			this.key = key;
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
			return key;
		}
	}

	@SuppressWarnings("deprecation")
	@Test
	public void upsertWithUserGeneratedKey() {
		final NewEntityTest entity = new NewEntityTest("test");
		template.upsert(entity, UpsertStrategy.REPLACE);
		assertThat(template.collection(NewEntityTest.class).count(), is(1L));
		entity.setPersisted(true);
		template.upsert(entity, UpsertStrategy.REPLACE);
		assertThat(template.collection(NewEntityTest.class).count(), is(1L));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void mutliUpsertWithUserGeneratedKey() {
		final NewEntityTest entity1 = new NewEntityTest("test1");
		final NewEntityTest entity2 = new NewEntityTest("test2");
		template.upsert(Arrays.asList(entity1, entity2), UpsertStrategy.REPLACE);
		assertThat(template.collection(NewEntityTest.class).count(), is(2L));
		entity1.setPersisted(true);
		entity2.setPersisted(true);
		template.upsert(Arrays.asList(entity1, entity2), UpsertStrategy.REPLACE);
		assertThat(template.collection(NewEntityTest.class).count(), is(2L));
	}

}
