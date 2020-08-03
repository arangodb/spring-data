package com.arangodb.springframework.repository;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.arangodb.springframework.testdata.Address;
import com.arangodb.springframework.testdata.Customer;
import com.arangodb.springframework.testdata.ShoppingCart;

/**
 * Created by F625633 on 06/07/2017.
 */
public class ArangoRepositoryTest extends AbstractArangoRepositoryTest {

	@Test
	public void findOneTest() {
		repository.save(john);
		final String id = john.getId();
		Customer customer = repository.findById(id).get();
		assertEquals("customers do not match", john, customer);
		john.setAge(100);
		repository.save(john);
		customer = repository.findById(id).get();
		assertEquals("customers do not match", john, customer);
	}

	@Test
	public void saveAllTest() {
		repository.saveAll(customers);
		Iterable<Customer> docs = repository.findAll();
		docs.forEach(d -> d.setName("saveAllTest"));
		repository.saveAll(docs);
		repository.findAll().forEach(it -> assertEquals("name does not match", "saveAllTest", it.getName()));
	}

	@Test
	public void findAllByIterableTest() {
		repository.saveAll(customers);
		ids.add(john.getId());
		ids.add(bob.getId());
		final Iterable<Customer> response = repository.findAllById(ids);
		assertTrue("customers do not match", equals(customers, response, cmp, eq, false));
	}

	@Test
	public void findAllTest() {
		repository.saveAll(customers);
		final Iterable<Customer> response = repository.findAll();
		assertTrue("customers do not match", equals(customers, response, cmp, eq, false));
	}

	@Test
	public void countTest() {
		repository.saveAll(customers);
		final Long size = repository.count();
		assertTrue("customer set sizes do not match", customers.size() == size);
	}

	@Test
	public void existsTest() {
		repository.save(john);
		assertTrue("customer does not exist but should", repository.existsById(john.getId()));
		assertFalse("customer exists but should not", repository.existsById(john.getId() + "0"));
	}

	@Test
	public void deleteByEntityTest() {
		repository.saveAll(customers);
		final String johnId = john.getId();
		repository.delete(john);
		assertTrue(repository.existsById(bob.getId()));
		assertFalse(repository.existsById(johnId));
	}

	@Test
	public void deleteByIdTest() {
		repository.saveAll(customers);
		final String johnId = john.getId();
		repository.deleteById(johnId);
		assertTrue(repository.existsById(bob.getId()));
		assertFalse(repository.existsById(johnId));
	}

	@Test
	public void deleteByIterableTest() {
		repository.saveAll(customers);
		final List<Customer> toDelete = new ArrayList<>();
		toDelete.add(john);
		final String johnId = john.getId();
		repository.deleteAll(toDelete);
		assertTrue(repository.existsById(bob.getId()));
		assertFalse(repository.existsById(johnId));
	}

	@Test
	public void deleteAllTest() {
		repository.saveAll(customers);
		repository.deleteAll();
		assertEquals(0, repository.count());
	}

	@Test
	public void findAllSortTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("A", "Z", 0));
		toBeRetrieved.add(new Customer("B", "C", 0));
		toBeRetrieved.add(new Customer("B", "D", 0));
		repository.saveAll(toBeRetrieved);
		final List<Sort.Order> orders = new LinkedList<>();
		orders.add(new Sort.Order(Sort.Direction.ASC, "name"));
		orders.add(new Sort.Order(Sort.Direction.ASC, "surname"));
		final Sort sort = Sort.by(orders);
		final Iterable<Customer> retrieved = repository.findAll(sort);
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
	}

	@Test
	public void findAllPageableTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("A", "Z", 0));
		toBeRetrieved.add(new Customer("B", "X", 0));
		toBeRetrieved.add(new Customer("B", "Y", 0));
		toBeRetrieved.add(new Customer("C", "V", 0));
		toBeRetrieved.add(new Customer("D", "T", 0));
		toBeRetrieved.add(new Customer("D", "U", 0));
		toBeRetrieved.add(new Customer("E", "S", 0));
		toBeRetrieved.add(new Customer("F", "P", 0));
		toBeRetrieved.add(new Customer("F", "Q", 0));
		toBeRetrieved.add(new Customer("F", "R", 0));
		repository.saveAll(toBeRetrieved);
		final List<Sort.Order> orders = new LinkedList<>();
		orders.add(new Sort.Order(Sort.Direction.ASC, "name"));
		orders.add(new Sort.Order(Sort.Direction.ASC, "surname"));
		final Sort sort = Sort.by(orders);
		final int pageNumber = 1;
		final int pageSize = 3;
		final Page<Customer> retrievedPage = repository.findAll(PageRequest.of(pageNumber, pageSize, sort));
		assertEquals(toBeRetrieved.size(), retrievedPage.getTotalElements());
		assertEquals(pageNumber, retrievedPage.getNumber());
		assertEquals(pageSize, retrievedPage.getSize());
		assertEquals((toBeRetrieved.size() + pageSize - 1) / pageSize, retrievedPage.getTotalPages());
		final List<Customer> expected = toBeRetrieved.subList(pageNumber * pageSize, (pageNumber + 1) * pageSize);
		assertTrue(equals(expected, retrievedPage, cmp, eq, true));
	}

	@Test
	public void findOneByExampleTest() {
		repository.save(john);
		final Customer customer = repository.findOne(Example.of(john)).get();
		assertEquals("customers do not match", john, customer);
	}

	@Test
	public void findAllByExampleTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer check1 = new Customer("B", "X", 0);
		final Customer check2 = new Customer("B", "Y", 0);
		toBeRetrieved.add(new Customer("A", "Z", 0));
		toBeRetrieved.add(check1);
		toBeRetrieved.add(check2);
		toBeRetrieved.add(new Customer("C", "V", 0));
		repository.saveAll(toBeRetrieved);
		final Example<Customer> example = Example.of(new Customer("B", null, 0));
		final Iterable<?> retrieved = repository.findAll(example);
		final List<Customer> retrievedList = new LinkedList<>();
		retrieved.forEach(e -> retrievedList.add((Customer) e));
		final List<Customer> checkList = new LinkedList<>();
		checkList.add(check1);
		checkList.add(check2);
		assertTrue(equals(checkList, retrievedList, cmp, eq, false));
	}

	@Test
	public void findAllByExampleRegexTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer check1 = new Customer("B", "X", 0);
		final Customer check2 = new Customer("B", "Y", 0);
		toBeRetrieved.add(new Customer("A", "Z", 0));
		toBeRetrieved.add(check1);
		toBeRetrieved.add(check2);
		toBeRetrieved.add(new Customer("C", "V", 0));
		repository.saveAll(toBeRetrieved);
		Customer find = new Customer("([B])", null, 0);
		ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnoreNullValues().withMatcher("name",
				match -> match.regex());
		final Example<Customer> example = Example.of(find, exampleMatcher);
		final Iterable<?> retrieved = repository.findAll(example);
		final List<Customer> retrievedList = new LinkedList<>();
		retrieved.forEach(e -> retrievedList.add((Customer) e));
		final List<Customer> checkList = new LinkedList<>();
		checkList.add(check1);
		checkList.add(check2);
		assertTrue(equals(checkList, retrievedList, cmp, eq, false));
	}

	@Test
	public void findAllSortByExampleTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("A", "Z", 0));
		toBeRetrieved.add(new Customer("B", "C", 0));
		toBeRetrieved.add(new Customer("B", "D", 0));
		repository.saveAll(toBeRetrieved);
		repository.save(new Customer("A", "A", 1));
		final Example<Customer> example = Example.of(new Customer("", "", 0), ExampleMatcher.matchingAny()
				.withIgnoreNullValues().withIgnorePaths(new String[] { "location", "alive" }));
		final List<Sort.Order> orders = new LinkedList<>();
		orders.add(new Sort.Order(Sort.Direction.ASC, "name"));
		orders.add(new Sort.Order(Sort.Direction.ASC, "surname"));
		final Sort sort = Sort.by(orders);
		final Iterable<Customer> retrieved = repository.findAll(example, sort);
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
	}

	@Test
	public void findAllPageableByExampleTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("A", "Z", 0));
		toBeRetrieved.add(new Customer("B", "X", 0));
		toBeRetrieved.add(new Customer("B", "Y", 0));
		toBeRetrieved.add(new Customer("C", "V", 0));
		toBeRetrieved.add(new Customer("D", "T", 0));
		toBeRetrieved.add(new Customer("D", "U", 0));
		toBeRetrieved.add(new Customer("E", "S", 0));
		toBeRetrieved.add(new Customer("F", "P", 0));
		toBeRetrieved.add(new Customer("F", "Q", 0));
		toBeRetrieved.add(new Customer("F", "R", 0));
		repository.saveAll(toBeRetrieved);
		repository.save(new Customer("A", "A", 1));
		final Example<Customer> example = Example.of(new Customer("", "", 0), ExampleMatcher.matchingAny()
				.withIgnoreNullValues().withIgnorePaths(new String[] { "location", "alive" }));
		final List<Sort.Order> orders = new LinkedList<>();
		orders.add(new Sort.Order(Sort.Direction.ASC, "name"));
		orders.add(new Sort.Order(Sort.Direction.ASC, "surname"));
		final Sort sort = Sort.by(orders);
		final int pageNumber = 1;
		final int pageSize = 3;
		final Page<Customer> retrievedPage = repository.findAll(example, PageRequest.of(pageNumber, pageSize, sort));
		assertEquals(toBeRetrieved.size(), retrievedPage.getTotalElements());
		assertEquals(pageNumber, retrievedPage.getNumber());
		assertEquals(pageSize, retrievedPage.getSize());
		assertEquals((toBeRetrieved.size() + pageSize - 1) / pageSize, retrievedPage.getTotalPages());
		final List<Customer> expected = toBeRetrieved.subList(pageNumber * pageSize, (pageNumber + 1) * pageSize);
		assertTrue(equals(expected, retrievedPage, cmp, eq, true));
	}

	@Test
	public void countByExampleTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("A", "Z", 0));
		toBeRetrieved.add(new Customer("B", "X", 0));
		toBeRetrieved.add(new Customer("B", "Y", 0));
		toBeRetrieved.add(new Customer("C", "V", 0));
		toBeRetrieved.add(new Customer("D", "T", 0));
		toBeRetrieved.add(new Customer("D", "U", 0));
		toBeRetrieved.add(new Customer("E", "S", 0));
		toBeRetrieved.add(new Customer("F", "P", 0));
		toBeRetrieved.add(new Customer("F", "Q", 0));
		toBeRetrieved.add(new Customer("F", "R", 0));
		repository.saveAll(toBeRetrieved);
		final Example<Customer> example = Example.of(new Customer("", "", 0), ExampleMatcher.matchingAny()
				.withIgnoreNullValues().withIgnorePaths(new String[] { "location", "alive" }));
		final long size = repository.count(example);
		assertTrue(size == toBeRetrieved.size());
	}

	@Test
	public void existsByExampleTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer checker = new Customer("I", "Exist", 25, true);
		toBeRetrieved.add(new Customer("A", "Z", 0));
		toBeRetrieved.add(new Customer("B", "X", 0));
		toBeRetrieved.add(new Customer("B", "Y", 0));
		toBeRetrieved.add(new Customer("C", "V", 0));
		toBeRetrieved.add(checker);
		repository.saveAll(toBeRetrieved);
		final Example<Customer> example = Example.of(checker);
		final boolean exists = repository.exists(example);
		assertTrue(exists);
	}

	@Test
	public void startingWithByExampleTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer check = new Customer("Abba", "Bbaaaa", 100);
		toBeRetrieved.add(check);
		toBeRetrieved.add(new Customer("Baabba", "", 67));
		toBeRetrieved.add(new Customer("B", "", 43));
		toBeRetrieved.add(new Customer("C", "", 76));
		repository.saveAll(toBeRetrieved);
		final Example<Customer> example = Example.of(new Customer(null, "bb", 100), ExampleMatcher.matching()
				.withMatcher("surname", match -> match.startsWith()).withIgnoreCase("surname").withIgnoreNullValues());
		final Customer retrieved = repository.findOne(example).get();
		assertEquals(check, retrieved);
	}
	
	@Test
	public void findAllByExampleWithArrayTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer check = new Customer("Abba", "Bbaaaa", 100);
		final Customer nested = new Customer("Bwa?[a.b]baAa", "", 67);
		final Customer nested2 = new Customer("qwerty", "", 10);
		check.setNestedCustomers(Arrays.asList(nested, nested2));
		toBeRetrieved.add(check);
		toBeRetrieved.add(new Customer("Baabba", "", 67));
		toBeRetrieved.add(new Customer("B", "", 43));
		toBeRetrieved.add(new Customer("C", "", 76));
		repository.saveAll(toBeRetrieved);
		final Customer exampleCustomer = new Customer("Abba", "Bbaaaa", 100);
		final Customer exampleNested = new Customer("Bwa?[a.b]baAa", "", 67);
		exampleCustomer.setNestedCustomers(Arrays.asList(exampleNested));
		final Example<Customer> example = Example.of(exampleCustomer);
		final Customer retrieved = repository.findOne(example).get();
		assertEquals(check, retrieved);
	}

	@Test
	public void findAllByExampleWithArray2Test() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer check = new Customer("Abba", "Bbaaaa", 100);
		final Customer nested = new Customer("Bwa?[a.b]baAa", "", 67);
		final Customer nested2 = new Customer("qwertyASD", "", 10);
		check.setNestedCustomers(Arrays.asList(nested, nested2));
		toBeRetrieved.add(check);
		toBeRetrieved.add(new Customer("Baabba", "", 67));
		toBeRetrieved.add(new Customer("B", "", 43));
		toBeRetrieved.add(new Customer("C", "", 76));
		repository.saveAll(toBeRetrieved);
		final Customer exampleCustomer = new Customer();
		final Customer exampleNested = new Customer("qwertyASD", "", 10);
		exampleCustomer.setNestedCustomers(Arrays.asList(exampleNested));
		final Example<Customer> example = Example.of(exampleCustomer, ExampleMatcher.matching()
				.withIgnoreNullValues().withIgnorePaths(new String[] { "location", "alive", "age" }));
		final Customer retrieved = repository.findOne(example).get();
		assertEquals(check, retrieved);
	}
	
	@Test
	public void findAllByExampleWithArrayORTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer check = new Customer("Abba", "Bbaaaa", 100);
		final Customer nested = new Customer("Bwa?[a.b]baAa", "", 67);
		final Customer nested2 = new Customer("qwertyASD", "", 10);
		check.setNestedCustomers(Arrays.asList(nested, nested2));
		toBeRetrieved.add(check);
		toBeRetrieved.add(new Customer("Baabba", "", 67));
		toBeRetrieved.add(new Customer("B", "", 43));
		toBeRetrieved.add(new Customer("C", "", 76));
		repository.saveAll(toBeRetrieved);
		final Customer exampleCustomer = new Customer();
		final Customer exampleNested = new Customer("qwertyASD", "", 10);
		final Customer exampleOr = new Customer("qwertyOr", "", 10);
		exampleCustomer.setNestedCustomers(Arrays.asList(exampleNested, exampleOr));
		final Example<Customer> example = Example.of(exampleCustomer, ExampleMatcher.matching()
				.withIgnoreNullValues().withIgnorePaths(new String[] { "location", "alive", "age" }));
		final Customer retrieved = repository.findOne(example).get();
		assertEquals(check, retrieved);
	}

	

	@Test
	public void endingWithByExampleNestedTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer check = new Customer("Abba", "Bbaaaa", 100);
		final Customer nested = new Customer("$B*\\wa?[a.b]baaa", "", 67);
		final Customer nested2 = new Customer("qwerty", "", 10);
		nested2.setAddress(new Address("123456"));
		nested.setNestedCustomer(nested2);
		check.setNestedCustomer(nested);
		toBeRetrieved.add(check);
		toBeRetrieved.add(new Customer("B", "", 43));
		toBeRetrieved.add(new Customer("C", "", 76));
		repository.saveAll(toBeRetrieved);
		final Customer exampleCustomer = new Customer("Abba", "Bbaaaa", 100);
		final Customer nested3 = new Customer("B*\\wa?[a.b]baAa", null, 66);
		nested3.setNestedCustomer(nested2);
		exampleCustomer.setNestedCustomer(nested3);
		final Example<Customer> example = Example.of(exampleCustomer,
				ExampleMatcher.matching().withMatcher("nestedCustomer.name", match -> match.endsWith())
						.withIgnoreCase("nestedCustomer.name").withIgnoreNullValues().withTransformer(
								"nestedCustomer.age", o -> Optional.of(Integer.valueOf(o.get().toString()) + 1)));
		final Customer retrieved = repository.findOne(example).get();
		assertEquals(check, retrieved);
	}

	@Test
	public void endingWithByExampleNestedIncludeNullTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer check = new Customer("Abba", "Bbaaaa", 100);
		final Customer nested = new Customer("$B*\\wa?[a.b]baaa", "", 67);
		final Customer nested2 = new Customer("qwerty", "", 10);
		nested2.setAddress(new Address("123456"));
		nested.setNestedCustomer(nested2);
		check.setNestedCustomer(nested);
		toBeRetrieved.add(check);
		toBeRetrieved.add(new Customer("B", "", 43));
		toBeRetrieved.add(new Customer("C", "", 76));
		repository.saveAll(toBeRetrieved);
		final Customer exampleCustomer = new Customer("Abba", "Bbaaaa", 100);
		final Customer nested3 = new Customer("B*\\wa?[a.b]baAa", "", 66);
		nested3.setNestedCustomer(nested2);
		exampleCustomer.setNestedCustomer(nested3);
		final Example<Customer> example = Example.of(exampleCustomer,
				ExampleMatcher.matching().withMatcher("nestedCustomer.name", match -> match.endsWith())
						.withIgnorePaths(new String[] { "arangoId", "id", "key", "rev" })
						.withIgnoreCase("nestedCustomer.name").withIncludeNullValues().withTransformer(
								"nestedCustomer.age", o -> Optional.of(Integer.valueOf(o.get().toString()) + 1)));
		final Customer retrieved = repository.findOne(example).get();
		assertEquals(check, retrieved);
	}

	@Test
	public void containingExampleTest() {
		final Customer entity = new Customer("name", "surname", 10);
		repository.save(entity);

		final Customer probe = new Customer();
		probe.setName("am");
		final Example<Customer> example = Example.of(probe,
				ExampleMatcher.matching().withStringMatcher(StringMatcher.CONTAINING).withIgnorePaths("arangoId", "id",
						"key", "rev", "surname", "age"));
		final Optional<Customer> retrieved = repository.findOne(example);
		assertThat(retrieved.isPresent(), is(true));
		assertThat(retrieved.get().getName(), is("name"));
	}

	@Test
	public void plusSignExampleTest() {
		String NAME = "Abc+Def";
		final Customer entity = new Customer(NAME, "surname", 10);
		repository.save(entity);

		final Customer probe = new Customer();
		probe.setName(NAME);
		probe.setAge(10);
		final Example<Customer> example = Example.of(probe);
		final Optional<Customer> retrieved = repository.findOne(example);
		assertThat(retrieved.isPresent(), is(true));
		assertThat(retrieved.get().getName(), is(NAME));
	}

	@Test
	public void exampleWithLikeAndEscapingTest() {
		String NAME = "Abc+Def";
		final Customer entity = new Customer(NAME, "surname", 10);
		repository.save(entity);

		final Customer probe = new Customer();
		probe.setName("Abc+De%");
		probe.setAge(10);
		final Example<Customer> example = Example.of(probe);
		final Optional<Customer> retrieved = repository.findOne(example);
		assertThat(retrieved.isPresent(), is(false));
	}

	@Test
	public void exampleWithRefPropertyTest() {

		ShoppingCart shoppingCart = new ShoppingCart();
		shoppingCartRepository.save(shoppingCart);

		Customer customer = new Customer("Dhiren", "Upadhyay", 28);
		customer.setShoppingCart(shoppingCart);
		repository.save(customer);

		Customer customer1 = new Customer();
		customer1.setShoppingCart(shoppingCart);
		ExampleMatcher exampleMatcher = ExampleMatcher.matching().withIgnorePaths("age", "location", "alive");
		Example<Customer> example = Example.of(customer1, exampleMatcher);

		final Customer retrieved = repository.findOne(example).orElse(null);
		assertEquals(customer, retrieved);
	}

}
