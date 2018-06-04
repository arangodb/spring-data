package com.arangodb.springframework.repository.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.isOneOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.BaseDocument;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.core.convert.DBDocumentEntity;
import com.arangodb.springframework.repository.AbstractArangoRepositoryTest;
import com.arangodb.springframework.testdata.Customer;
import com.arangodb.springframework.testdata.CustomerNameProjection;

/**
 * 
 * @author Audrius Malele
 * @author Mark McCormick
 * @author Mark Vollmary
 * @author Christian Lechner
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ArangoAqlQueryTest extends AbstractArangoRepositoryTest {

	@Test
	public void findOneByIdAqlWithNamedParameterTest() {
		repository.saveAll(customers);
		final Map<String, Object> retrieved = repository.findOneByIdAqlWithNamedParameter(john.getId(),
			new AqlQueryOptions());
		final Customer retrievedCustomer = template.getConverter().read(Customer.class,
			new DBDocumentEntity(retrieved));
		assertEquals(john, retrievedCustomer);
	}

	@Test
	public void findOneByIdAndNameAqlTest() {
		repository.saveAll(customers);
		final BaseDocument retrieved = repository.findOneByIdAndNameAql(john.getId(), john.getName());
		final Map<String, Object> allProperties = new HashMap<>();
		allProperties.put("_id", retrieved.getId());
		allProperties.put("_key", retrieved.getKey());
		allProperties.put("_rev", retrieved.getRevision());
		retrieved.getProperties().forEach((k, v) -> allProperties.put(k, v));
		final Customer retrievedCustomer = template.getConverter().read(Customer.class,
			new DBDocumentEntity(allProperties));
		assertEquals(john, retrievedCustomer);
	}

	@Test
	public void findOneByBindVarsAqlTest() {
		repository.saveAll(customers);
		final Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("id", john.getId());
		bindVars.put("name", john.getName());
		final ArangoCursor<Customer> retrieved = repository
				.findOneByBindVarsAql(new AqlQueryOptions().ttl(127).cache(true), bindVars);
		assertEquals(john, retrieved.next());
	}

	@Test
	public void findOneByComplementingNameAndBindVarsAqlTest() {
		repository.saveAll(customers);
		final Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("id", john.getId());
		final Customer retrieved = repository.findOneByNameAndBindVarsAql(john.getName(), bindVars);
		assertEquals(john, retrieved);
	}

	@Test
	public void findOneByOverridingNameAndBindVarsAqlTest() {
		repository.saveAll(customers);
		final Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("id", john.getId());
		bindVars.put("name", john.getId() + "random");
		final Customer retrieved = repository.findOneByNameAndBindVarsAql(john.getName(), bindVars);
		assertEquals(john, retrieved);
	}

	@Test
	public void findOneByIdAndNameWithBindVarsAqlTest() {
		repository.saveAll(customers);
		final Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("id", john.getId());
		bindVars.put("0", john.getName() + "random");
		final Customer retrieved = repository.findOneByIdAndNameWithBindVarsAql(john.getName(), bindVars);
		assertEquals(john, retrieved);
	}

	@Test(expected = ArangoDBException.class)
	public void findOneByIdInCollectionAqlWithUnusedParamTest() {
		repository.saveAll(customers);
		final Customer retrieved = repository.findOneByIdInCollectionAqlWithUnusedParam(john.getId().split("/")[0],
			john.getId(), john.getId());
		assertEquals(john, retrieved);
	}

	@Test(expected = ArangoDBException.class)
	public void findOneByIdInNamedCollectionAqlWithUnusedParamTest() {
		repository.saveAll(customers);
		final Customer retrieved = repository.findOneByIdInNamedCollectionAqlWithUnusedParam(john.getId().split("/")[0],
			john.getId(), john.getId());
		assertEquals(john, retrieved);
	}

	@Test(expected = ArangoDBException.class)
	public void findOneByIdInIncorrectNamedCollectionAqlTest() {
		repository.saveAll(customers);
		final Customer retrieved = repository.findOneByIdInIncorrectNamedCollectionAql(john.getId().split("/")[0],
			john.getId(), john.getId());
		assertEquals(john, retrieved);
	}

	@Test(expected = ArangoDBException.class)
	public void findOneByIdInNamedCollectionAqlRejectedTest() {
		repository.saveAll(customers);
		final Customer retrieved = repository.findOneByIdInNamedCollectionAqlRejected(john.getId().split("/")[0],
			john.getId());
		assertEquals(john, retrieved);
	}

	@Test
	public void findManyBySurnameTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("James", "Smith", 35));
		toBeRetrieved.add(new Customer("Matt", "Smith", 34));
		repository.saveAll(toBeRetrieved);
		final List<Customer> retrieved = repository.findManyBySurname("Smith");
		assertTrue(equals(retrieved, toBeRetrieved, cmp, eq, false));

	}

	@Test
	public void queryCount() {
		assertEquals(repository.queryCount(Customer.class), 0L);
	}

	@Test
	public void queryDate() {
		assertEquals(repository.queryDate(), Instant.ofEpochMilli(1474988621));
	}

	@Test
	public void findOneByIdNamedQueryTest() {
		repository.saveAll(customers);
		final Customer retrieved = repository.findOneByIdNamedQuery(john.getId());
		assertEquals(john, retrieved);
	}

	@Test
	public void findOneByIdWithStaticProjectionTest() {
		repository.saveAll(customers);
		final CustomerNameProjection retrieved = repository.findOneByIdWithStaticProjection(john.getId());
		assertEquals(retrieved.getName(), john.getName());
	}

	@Test
	public void findManyLegalAgeWithStaticProjectionTest() {
		repository.saveAll(customers);
		final List<CustomerNameProjection> retrieved = repository.findManyLegalAgeWithStaticProjection();
		for (CustomerNameProjection proj : retrieved) {
			assertThat(proj.getName(), isOneOf(john.getName(), bob.getName()));
		}
	}

	@Test
	public void findOneByIdWithDynamicProjectionTest() {
		repository.saveAll(customers);
		final CustomerNameProjection retrieved = repository.findOneByIdWithDynamicProjection(john.getId(),
			CustomerNameProjection.class);
		assertEquals(retrieved.getName(), john.getName());
	}

	@Test
	public void findManyLegalAgeWithDynamicProjectionTest() {
		repository.saveAll(customers);
		final List<CustomerNameProjection> retrieved = repository
				.findManyLegalAgeWithDynamicProjection(CustomerNameProjection.class);
		for (CustomerNameProjection proj : retrieved) {
			assertThat(proj.getName(), isOneOf(john.getName(), bob.getName()));
		}
	}

	@Test
	public void pageableTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		repository.save(new Customer("A", "A", 0));
		repository.save(new Customer("A", "A", 1));
		toBeRetrieved.add(new Customer("A", "A", 2));
		repository.save(new Customer("B", "B", 3));
		toBeRetrieved.add(new Customer("A", "A", 4));
		repository.save(new Customer("A", "A", 5));
		repository.saveAll(toBeRetrieved);
		final Pageable pageable = PageRequest.of(1, 2, Sort.by("c.age"));
		final Page<Customer> retrieved = repository.findByNameAndSurnameWithPageable(pageable, "A", "A");
		assertThat(retrieved.getTotalElements(), is(5L));
		assertThat(retrieved.getTotalPages(), is(3));
		assertThat(retrieved.getContent(), is(toBeRetrieved));
	}

	@Test
	public void sortTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("A", "B", 2));
		toBeRetrieved.add(new Customer("A", "B", 3));
		toBeRetrieved.add(new Customer("A", "A", 1));
		repository.save(toBeRetrieved.get(1));
		repository.save(toBeRetrieved.get(0));
		repository.save(toBeRetrieved.get(2));
		repository.save(new Customer("C", "C", 0));
		final List<Customer> retrieved = repository
				.findByNameWithSort(Sort.by(Direction.DESC, "c.surname").and(Sort.by("c.age")), "A");
		assertThat(retrieved, is(toBeRetrieved));
	}

}
