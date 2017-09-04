package com.arangodb.springframework.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.springframework.testdata.Address;
import com.arangodb.springframework.testdata.Customer;

/**
 * Created by F625633 on 06/07/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ArangoRepositoryTest extends AbstractArangoRepositoryTest {

	@Test
	public void findOneTest() {
		repository.save(john);
		final String id = john.getId();
		Customer customer = repository.findOne(id);
		assertEquals("customers do not match", john, customer);
		john.setAge(100);
		repository.save(john);
		customer = repository.findOne(id);
		assertEquals("customers do not match", john, customer);
	}

	@Test
	public void findAllByIterableTest() {
		repository.save(customers);
		ids.add(john.getId());
		ids.add(bob.getId());
		final Iterable<Customer> response = repository.findAll(ids);
		assertTrue("customers do not match", equals(customers, response, cmp, eq, false));
	}

	@Test
	public void findAllTest() {
		repository.save(customers);
		final Iterable<Customer> response = repository.findAll();
		assertTrue("customers do not match", equals(customers, response, cmp, eq, false));
	}

	@Test
	public void countTest() {
		repository.save(customers);
		final Long size = repository.count();
		assertTrue("customer set sizes do not match", customers.size() == size);
	}

	@Test
	public void existsTest() {
		repository.save(john);
		assertTrue("customer does not exist but should", repository.exists(john.getId()));
		assertFalse("customer exists but should not", repository.exists(john.getId() + "0"));
	}

	@Test
	public void deleteByEntityTest() {
		repository.save(customers);
		final String johnId = john.getId();
		repository.delete(john);
		assertTrue(repository.exists(bob.getId()));
		assertFalse(repository.exists(johnId));
	}

	@Test
	public void deleteByIdTest() {
		repository.save(customers);
		final String johnId = john.getId();
		repository.delete(johnId);
		assertTrue(repository.exists(bob.getId()));
		assertFalse(repository.exists(johnId));
	}

	@Test
	public void deleteByIterableTest() {
		repository.save(customers);
		final List<Customer> toDelete = new ArrayList<>();
		toDelete.add(john);
		final String johnId = john.getId();
		repository.delete(toDelete);
		assertTrue(repository.exists(bob.getId()));
		assertFalse(repository.exists(johnId));
	}

	@Test
	public void deleteAllTest() {
		repository.save(customers);
		repository.deleteAll();
		assertTrue(repository.count() == 0);
	}

	@Test
	public void findAllSortTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("A", "Z", 0));
		toBeRetrieved.add(new Customer("B", "C", 0));
		toBeRetrieved.add(new Customer("B", "D", 0));
		repository.save(toBeRetrieved);
		final List<Sort.Order> orders = new LinkedList<>();
		orders.add(new Sort.Order(Sort.Direction.ASC, "name"));
		orders.add(new Sort.Order(Sort.Direction.ASC, "surname"));
		final Sort sort = new Sort(orders);
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
		repository.save(toBeRetrieved);
		final List<Sort.Order> orders = new LinkedList<>();
		orders.add(new Sort.Order(Sort.Direction.ASC, "name"));
		orders.add(new Sort.Order(Sort.Direction.ASC, "surname"));
		final Sort sort = new Sort(orders);
		final int pageNumber = 1;
		final int pageSize = 3;
		final Page<Customer> retrievedPage = repository.findAll(new PageRequest(pageNumber, pageSize, sort));
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
		final Customer customer = repository.findOne(Example.of(john));
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
		repository.save(toBeRetrieved);
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
	public void findAllSortByExampleTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("A", "Z", 0));
		toBeRetrieved.add(new Customer("B", "C", 0));
		toBeRetrieved.add(new Customer("B", "D", 0));
		repository.save(toBeRetrieved);
		repository.save(new Customer("A", "A", 1));
		final Example<Customer> example = Example.of(new Customer("", "", 0),
			ExampleMatcher.matchingAny().withIgnoreNullValues().withIgnorePaths(new String[] { "location", "alive" }));
		final List<Sort.Order> orders = new LinkedList<>();
		orders.add(new Sort.Order(Sort.Direction.ASC, "name"));
		orders.add(new Sort.Order(Sort.Direction.ASC, "surname"));
		final Sort sort = new Sort(orders);
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
		repository.save(toBeRetrieved);
		repository.save(new Customer("A", "A", 1));
		final Example<Customer> example = Example.of(new Customer("", "", 0),
			ExampleMatcher.matchingAny().withIgnoreNullValues().withIgnorePaths(new String[] { "location", "alive" }));
		final List<Sort.Order> orders = new LinkedList<>();
		orders.add(new Sort.Order(Sort.Direction.ASC, "name"));
		orders.add(new Sort.Order(Sort.Direction.ASC, "surname"));
		final Sort sort = new Sort(orders);
		final int pageNumber = 1;
		final int pageSize = 3;
		final Page<Customer> retrievedPage = repository.findAll(example, new PageRequest(pageNumber, pageSize, sort));
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
		repository.save(toBeRetrieved);
		final Example<Customer> example = Example.of(new Customer("", "", 0),
			ExampleMatcher.matchingAny().withIgnoreNullValues().withIgnorePaths(new String[] { "location", "alive" }));
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
		repository.save(toBeRetrieved);
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
		repository.save(toBeRetrieved);
		final Example<Customer> example = Example.of(new Customer(null, "bb", 100), ExampleMatcher.matching()
				.withMatcher("surname", match -> match.startsWith()).withIgnoreCase("surname").withIgnoreNullValues());
		final Customer retrieved = repository.findOne(example);
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
		repository.save(toBeRetrieved);
		final Customer exampleCustomer = new Customer("Abba", "Bbaaaa", 100);
		final Customer nested3 = new Customer("B*\\wa?[a.b]baAa", null, 66);
		nested3.setNestedCustomer(nested2);
		exampleCustomer.setNestedCustomer(nested3);
		final Example<Customer> example = Example.of(exampleCustomer,
			ExampleMatcher.matching().withMatcher("nestedCustomer.name", match -> match.endsWith())
					.withIgnoreCase("nestedCustomer.name").withIgnoreNullValues()
					.withTransformer("nestedCustomer.age", o -> ((int) o) + 1));
		final Customer retrieved = repository.findOne(example);
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
		repository.save(toBeRetrieved);
		final Customer exampleCustomer = new Customer("Abba", "Bbaaaa", 100);
		final Customer nested3 = new Customer("B*\\wa?[a.b]baAa", "", 66);
		nested3.setNestedCustomer(nested2);
		exampleCustomer.setNestedCustomer(nested3);
		final Example<Customer> example = Example.of(exampleCustomer,
			ExampleMatcher.matching().withMatcher("nestedCustomer.name", match -> match.endsWith())
					.withIgnorePaths(new String[] { "id", "key", "rev" }).withIgnoreCase("nestedCustomer.name")
					.withIncludeNullValues().withTransformer("nestedCustomer.age", o -> ((int) o) + 1));
		final Customer retrieved = repository.findOne(example);
		assertEquals(check, retrieved);
	}
}
