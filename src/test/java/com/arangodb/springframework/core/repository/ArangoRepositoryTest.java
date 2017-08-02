package com.arangodb.springframework.core.repository;

import com.arangodb.springframework.testdata.Customer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by F625633 on 06/07/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ArangoRepositoryTest extends AbstractArangoRepositoryTest {

	@Test
	public void findOneTest() {
		repository.save(john);
		Customer customer = repository.findOne(john.getId());
		assertEquals("customers do not match", john, customer);
	}

	@Test
	public void findAllByIterableTest() {
		repository.save(customers);
		ids.add(john.getId());
		ids.add(bob.getId());
		Iterable<Customer> response = repository.findAll(ids);
		assertTrue("customers do not match", equals(customers, response, cmp, eq, false));
	}

	@Test
	public void findAllTest() {
		repository.save(customers);
		Iterable<Customer> response = repository.findAll();
		assertTrue("customers do not match", equals(customers, response, cmp, eq, false));
	}

	@Test
	public void countTest() {
		repository.save(customers);
		Long size = repository.count();
		assertTrue("customer set sizes do not match", customers.size() == size);
	}

	@Test
	public void existsTest() {
		repository.save(john);
		assertTrue("customer does not exist but should",  repository.exists(john.getId()));
		assertFalse("customer exists but should not",  repository.exists(john.getId() + "0"));
	}

	@Test
	public void deleteByEntityTest() {
		repository.save(customers);
		String johnId = john.getId();
		repository.delete(john);
		assertTrue(repository.exists(bob.getId()));
		assertFalse(repository.exists(johnId));
	}

	@Test
	public void deleteByIdTest() {
		repository.save(customers);
		String johnId = john.getId();
		repository.delete(johnId);
		assertTrue(repository.exists(bob.getId()));
		assertFalse(repository.exists(johnId));
	}

	@Test
	public void deleteByIterableTest() {
		repository.save(customers);
		List<Customer> toDelete = new ArrayList<>();
		toDelete.add(john);
		String johnId = john.getId();
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
	public void findAllSort() {
		List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("A", "Z", 0));
		toBeRetrieved.add(new Customer("B", "C", 0));
		toBeRetrieved.add(new Customer("B", "D", 0));
		repository.save(toBeRetrieved);
		List<Sort.Order> orders = new LinkedList<>();
		orders.add(new Sort.Order(Sort.Direction.ASC, "name"));
		orders.add(new Sort.Order(Sort.Direction.ASC, "surname"));
		Sort sort = new Sort(orders);
		Iterable<Customer> retrieved = repository.findAll(sort);
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
	}

	@Test
	public void findAllPageable() {
		List<Customer> toBeRetrieved = new LinkedList<>();
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
		List<Sort.Order> orders = new LinkedList<>();
		orders.add(new Sort.Order(Sort.Direction.ASC, "name"));
		orders.add(new Sort.Order(Sort.Direction.ASC, "surname"));
		Sort sort = new Sort(orders);
		int pageNumber = 1;
		int pageSize = 3;
		Page<Customer> retrievedPage = repository.findAll(new PageRequest(pageNumber, pageSize, sort));
		assertEquals(toBeRetrieved.size(), retrievedPage.getTotalElements());
		assertEquals(pageNumber, retrievedPage.getNumber());
		assertEquals(pageSize, retrievedPage.getSize());
		assertEquals((toBeRetrieved.size() + pageSize - 1) / pageSize , retrievedPage.getTotalPages());
		List<Customer> expected = toBeRetrieved.subList(pageNumber * pageSize, (pageNumber + 1) * pageSize);
		assertTrue(equals(expected, retrievedPage, cmp, eq, true));
	}
}
