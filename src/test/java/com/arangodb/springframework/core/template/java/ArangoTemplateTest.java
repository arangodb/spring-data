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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
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
		final DocumentCreateEntity<Customer> res = template.insertDocument(new Customer("John", "Doe", 30));
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
		final MultiDocumentEntity<DocumentCreateEntity<Object>> res = template
				.insertDocuments(Arrays.asList(c1, c2, c3, c4), Customer.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(3));
		assertThat(res.getErrors().size(), is(1));
		assertThat(c1.getId(), is(notNullValue()));
		assertThat(c2.getId(), is(notNullValue()));
		assertThat(c3.getId(), is(notNullValue()));
	}

	@Test
	public void getDocument() {
		final DocumentCreateEntity<Customer> res = template
				.insertDocument(new Customer("John", "Doe", 30, new Address("22162–1010")));
		final Customer customer = template.getDocument(res.getId(), Customer.class);
		assertThat(customer, is(notNullValue()));
		assertThat(customer.getName(), is("John"));
		assertThat(customer.getSurname(), is("Doe"));
		assertThat(customer.getAge(), is(30));
		assertThat(customer.getAddress(), is(notNullValue()));
		assertThat(customer.getAddress().getZipCode(), is("22162–1010"));
	}

	@Test
	public void replaceDocument() {
		final DocumentCreateEntity<Customer> res = template.insertDocument(new Customer("John", "Doe", 30));
		final DocumentUpdateEntity<Customer> replaceDocument = template.replaceDocument(res.getId(),
			new Customer("Jane", "Doe", 26));
		assertThat(replaceDocument, is(notNullValue()));
		final Customer customer = template.getDocument(res.getId(), Customer.class);
		assertThat(customer, is(notNullValue()));
		assertThat(customer.getName(), is("Jane"));
		assertThat(customer.getSurname(), is("Doe"));
		assertThat(customer.getAge(), is(26));
	}

	@Test
	public void replaceDocuments() {
		final DocumentCreateEntity<Product> a = template.insertDocument(new Product("a"));
		final DocumentCreateEntity<Product> b = template.insertDocument(new Product("b"));

		final Product documentA = template.getDocument(a.getId(), Product.class);
		documentA.setName("aa");
		final Product documentB = template.getDocument(b.getId(), Product.class);
		documentB.setName("bb");

		final MultiDocumentEntity<DocumentUpdateEntity<Object>> res = template
				.replaceDocuments(Arrays.asList(documentA, documentB), Product.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(2));

		final Product newA = template.getDocument(a.getId(), Product.class);
		assertThat(newA.getName(), is("aa"));
		final Product newB = template.getDocument(b.getId(), Product.class);
		assertThat(newB.getName(), is("bb"));
	}

	@Test
	public void updateDocument() {
		final DocumentCreateEntity<Customer> res = template.insertDocument(new Customer("John", "Doe", 30));
		template.updateDocument(res.getId(), new Customer("Jane", "Doe", 26));
		final Customer customer = template.getDocument(res.getId(), Customer.class);
		assertThat(customer, is(notNullValue()));
		assertThat(customer.getName(), is("Jane"));
		assertThat(customer.getSurname(), is("Doe"));
		assertThat(customer.getAge(), is(26));
	}

	@Test
	public void updateDocuments() {
		final DocumentCreateEntity<Product> a = template.insertDocument(new Product("a"));
		final DocumentCreateEntity<Product> b = template.insertDocument(new Product("b"));

		final Product documentA = template.getDocument(a.getId(), Product.class);
		documentA.setName("aa");
		final Product documentB = template.getDocument(b.getId(), Product.class);
		documentB.setName("bb");

		final MultiDocumentEntity<DocumentUpdateEntity<Object>> res = template
				.updateDocuments(Arrays.asList(documentA, documentB), Product.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(2));

		final Product newA = template.getDocument(a.getId(), Product.class);
		assertThat(newA.getName(), is("aa"));
		final Product newB = template.getDocument(b.getId(), Product.class);
		assertThat(newB.getName(), is("bb"));
	}

	@Test
	public void deleteDocument() {
		final DocumentCreateEntity<Customer> res = template.insertDocument(new Customer("John", "Doe", 30));
		template.deleteDocument(res.getId(), Customer.class);
		final Customer customer = template.getDocument(res.getId(), Customer.class);
		assertThat(customer, is(nullValue()));
	}

	@Test
	public void deleteDocuments() {
		final DocumentCreateEntity<Product> a = template.insertDocument(new Product("a"));
		final DocumentCreateEntity<Product> b = template.insertDocument(new Product("b"));

		final Product documentA = template.getDocument(a.getId(), Product.class);
		final Product documentB = template.getDocument(b.getId(), Product.class);

		final MultiDocumentEntity<DocumentDeleteEntity<Product>> res = template
				.deleteDocuments(Arrays.asList(documentA, documentB), Product.class);
		assertThat(res, is(notNullValue()));
		assertThat(res.getDocuments().size(), is(2));

		final Product deletedA = template.getDocument(a.getId(), Product.class);
		assertThat(deletedA, is(nullValue()));
		final Product deletedB = template.getDocument(b.getId(), Product.class);
		assertThat(deletedB, is(nullValue()));
	}

	@Test
	public void query() {
		template.insertDocument(new Customer("John", "Doe", 30));
		final ArangoCursor<Customer> cursor = template.query("FOR c IN @@coll FILTER c.name == @name RETURN c",
			new MapBuilder().put("@coll", "customer").put("name", "John").get(), new AqlQueryOptions(), Customer.class);
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
		template.insertDocument(new Customer("John", "Doe", 30));
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
		template.insertDocument(new Customer("John", "Doe", 30));
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
		template.insertDocument(new Customer("John", "Doe", 30));
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

}
