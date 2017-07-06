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

package com.arangodb.springframework.core.mapping;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.testdata.Customer;
import com.arangodb.springframework.testdata.Knows;
import com.arangodb.springframework.testdata.Owns;
import com.arangodb.springframework.testdata.Person;
import com.arangodb.springframework.testdata.Product;
import com.arangodb.springframework.testdata.ShoppingCart;
import com.arangodb.util.MapBuilder;
import com.arangodb.velocypack.VPackSlice;

/**
 * @author Mark Vollmary
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class ArangoMappingTest extends AbstractArangoTest {

	@Test
	public void id() {
		final DocumentCreateEntity<ShoppingCart> ref = template.insertDocument(new ShoppingCart());
		final ShoppingCart cart = template.getDocument(ref.getId(), ShoppingCart.class);
		assertThat(cart, is(notNullValue()));
		assertThat(cart.getId(), is(ref.getId()));
	}

	@Test
	public void singleReference() {
		final DocumentCreateEntity<Customer> ref;
		{
			final ShoppingCart shoppingCart = new ShoppingCart();
			shoppingCart.setId(template.insertDocument(shoppingCart).getId());
			final Customer customer = new Customer();
			customer.setShoppingCart(shoppingCart);
			ref = template.insertDocument(customer);
		}
		final Customer customerDocument = template.getDocument(ref.getId(), Customer.class);
		assertThat(customerDocument, is(notNullValue()));
		final ShoppingCart shoppingCart = customerDocument.getShoppingCart();
		assertThat(shoppingCart, is(notNullValue()));
		assertThat(shoppingCart.getId(), is(notNullValue()));

		final ShoppingCart shoppingCartDocument = template.getDocument(shoppingCart.getId(), ShoppingCart.class);
		assertThat(shoppingCartDocument, is(notNullValue()));
		assertThat(shoppingCartDocument.getId(), is(shoppingCart.getId()));
	}

	@Test
	public void referenceList() {
		final DocumentCreateEntity<Customer> ref;
		{
			final Collection<Product> products = new ArrayList<>();
			{
				final Product productA = new Product("a");
				productA.setId(template.insertDocument(productA).getId());
				products.add(productA);
			}
			{
				final Product productB = new Product("b");
				productB.setId(template.insertDocument(productB).getId());
				products.add(productB);
			}
			{
				final Product productC = new Product("c");
				productC.setId(template.insertDocument(productC).getId());
				products.add(productC);
			}
			final ShoppingCart shoppingCart = new ShoppingCart();
			shoppingCart.setProducts(products);
			shoppingCart.setId(template.insertDocument(shoppingCart).getId());
			final Customer customer = new Customer();
			customer.setShoppingCart(shoppingCart);
			ref = template.insertDocument(customer);
		}
		final Customer customerDocument = template.getDocument(ref.getId(), Customer.class);
		final Collection<Product> products = customerDocument.getShoppingCart().getProducts();
		assertThat(products.size(), is(3));
		assertThat(products.stream().map(e -> e.getName()).collect(Collectors.toList()), hasItems("a", "b", "c"));
	}

	@Test
	public void existingReference() {
		final DocumentCreateEntity<Customer> ref1;
		{
			final ShoppingCart shoppingCart = new ShoppingCart();
			shoppingCart.setId(template.insertDocument(shoppingCart).getId());
			final Customer customer = new Customer();
			customer.setShoppingCart(shoppingCart);
			ref1 = template.insertDocument(customer);
		}
		final Customer customer1 = template.getDocument(ref1.getId(), Customer.class);
		final DocumentCreateEntity<Customer> ref2;
		{
			final Customer customer = new Customer();
			customer.setShoppingCart(customer1.getShoppingCart());
			ref2 = template.insertDocument(customer);
		}
		final Customer customer2 = template.getDocument(ref2.getId(), Customer.class);
		assertThat(customer1.getShoppingCart().getId(), is(customer2.getShoppingCart().getId()));
		assertThat(template.driver().db(ArangoTestConfiguration.DB).collection("shopping-cart").count().getCount(),
			is(1L));
	}

	@Test
	public void existingReferences() {
		final DocumentCreateEntity<Customer> ref1;
		{
			final Collection<Product> products = new ArrayList<>();
			{
				final Product productA = new Product("a");
				productA.setId(template.insertDocument(productA).getId());
				products.add(productA);
			}
			{
				final Product productB = new Product("b");
				productB.setId(template.insertDocument(productB).getId());
				products.add(productB);
			}
			final Product productC = new Product("c");
			productC.setId(template.insertDocument(productC).getId());
			products.add(productC);
			final ShoppingCart shoppingCart = new ShoppingCart();
			shoppingCart.setProducts(products);
			shoppingCart.setId(template.insertDocument(shoppingCart).getId());
			final Customer customer = new Customer();
			customer.setShoppingCart(shoppingCart);
			ref1 = template.insertDocument(customer);
		}
		final Customer customer1 = template.getDocument(ref1.getId(), Customer.class);
		final DocumentCreateEntity<Customer> ref2;
		{
			final Customer customer = new Customer();
			customer.setShoppingCart(customer1.getShoppingCart());
			{
				final Product productD = new Product("d");
				productD.setId(template.insertDocument(productD).getId());
				customer.getShoppingCart().getProducts().add(productD);
			}
			ref2 = template.insertDocument(customer);
		}
		final Customer customer2 = template.getDocument(ref2.getId(), Customer.class);
		assertThat(customer1.getShoppingCart().getId(), is(customer2.getShoppingCart().getId()));
		assertThat(template.driver().db(ArangoTestConfiguration.DB).collection("shopping-cart").count().getCount(),
			is(1L));
		assertThat(template.driver().db(ArangoTestConfiguration.DB).collection("product").count().getCount(), is(4L));
	}

	@Test
	public void query() {
		final DocumentCreateEntity<Customer> ref;
		{
			final Customer customer = new Customer();
			final Collection<Product> products = new ArrayList<>();
			{
				final Product productA = new Product("a");
				productA.setId(template.insertDocument(productA).getId());
				products.add(productA);
			}
			{
				final Product productB = new Product("b");
				productB.setId(template.insertDocument(productB).getId());
				products.add(productB);
			}
			{
				final Product productC = new Product("c");
				productC.setId(template.insertDocument(productC).getId());
				products.add(productC);
			}
			final ShoppingCart shoppingCart = new ShoppingCart();
			shoppingCart.setProducts(products);
			shoppingCart.setId(template.insertDocument(shoppingCart).getId());
			customer.setShoppingCart(shoppingCart);
			ref = template.insertDocument(customer);
		}
		final ArangoCursor<Customer> cursor = template.query("FOR c IN @@coll FILTER c._id == @id RETURN c",
			new MapBuilder().put("@coll", "customer").put("id", ref.getId()).get(), new AqlQueryOptions(),
			Customer.class);
		assertThat(cursor, is(notNullValue()));
		final List<Customer> customers = cursor.asListRemaining();
		assertThat(customers.size(), is(1));
		final Customer customer = customers.get(0);
		final ShoppingCart shoppingCart = customer.getShoppingCart();
		assertThat(shoppingCart, is(notNullValue()));
		final Collection<Product> products = shoppingCart.getProducts();
		assertThat(products, is(notNullValue()));
		assertThat(products.size(), is(3));
		assertThat(products.stream().map(e -> e.getName()).collect(Collectors.toList()), hasItems("a", "b", "c"));
	}

	@Test
	public void fieldNameAnnotation() {
		final Product p = new Product();
		p.setDesc("test");
		final DocumentCreateEntity<Product> res = template.insertDocument(p);
		final VPackSlice slice = template.driver().db(ArangoTestConfiguration.DB).getDocument(res.getId(),
			VPackSlice.class);
		assertThat(slice, is(notNullValue()));
		assertThat(slice.get("description").isString(), is(true));
		assertThat(slice.get("description").getAsString(), is("test"));
	}

	@Test
	public void relations() {
		final Product p1 = new Product();
		p1.setId(template.insertDocument(p1).getId());
		final Product p2 = new Product();
		p2.setId(template.insertDocument(p2).getId());

		final Customer customer = new Customer();
		customer.setId(template.insertDocument(customer).getId());
		template.insertDocument(new Owns(customer, p1));
		template.insertDocument(new Owns(customer, p2));

		final Customer doc = template.getDocument(customer.getId(), Customer.class);
		assertThat(doc.getOwns(), is(notNullValue()));
		assertThat(doc.getOwns().size(), is(2));
		assertThat(doc.getOwns().stream().map(p -> p.getId()).collect(Collectors.toList()),
			hasItems(p1.getId(), p2.getId()));
	}

	@Test
	@Ignore
	public void relationsLazy() {
		final Person p1 = new Person();
		p1.setId(template.insertDocument(p1).getId());
		final Person p2 = new Person();
		p2.setId(template.insertDocument(p2).getId());
		final Person p3 = new Person();
		p3.setId(template.insertDocument(p3).getId());

		template.insertDocument(new Knows(p1, p2));
		template.insertDocument(new Knows(p1, p3));

		final Person doc = template.getDocument(p1.getId(), Person.class);
		assertThat(doc.getKnows(), is(notNullValue()));
		assertThat(doc.getKnows().size(), is(2));
		assertThat(doc.getKnows().stream().map(e -> e.getId()).collect(Collectors.toList()),
			hasItems(p2.getId(), p3.getId()));
	}

	@Test
	public void edgeFrom() {
		final Product product = new Product();
		product.setId(template.insertDocument(product).getId());
		final Customer customer = new Customer();
		customer.setId(template.insertDocument(customer).getId());
		final DocumentCreateEntity<Owns> res = template.insertDocument(new Owns(customer, product));

		final Owns owns = template.getDocument(res.getId(), Owns.class);
		assertThat(owns, is(notNullValue()));
		assertThat(owns.getFrom(), is(notNullValue()));
		assertThat(owns.getFrom().getId(), is(customer.getId()));
		assertThat(owns.getTo(), is(notNullValue()));
		assertThat(owns.getTo().getId(), is(product.getId()));
	}
}
