package com.arangodb.springframework.repository.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.BaseDocument;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.core.convert.DBDocumentEntity;
import com.arangodb.springframework.repository.AbstractArangoRepositoryTest;
import com.arangodb.springframework.testdata.Customer;

/**
 * Created by F625633 on 12/07/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ArangoAqlQueryTest extends AbstractArangoRepositoryTest {

	private final AqlQueryOptions OPTIONS = new AqlQueryOptions();

	@Test
	public void findOneByIdAqlWithNamedParameterTest() {
		repository.save(customers);
		final Map<String, Object> retrieved = repository.findOneByIdAqlWithNamedParameter(john.getId(), OPTIONS);
		final Customer retrievedCustomer = template.getConverter().read(Customer.class,
			new DBDocumentEntity(retrieved));
		assertEquals(john, retrievedCustomer);
	}

	@Test
	public void findOneByIdAndNameAqlTest() {
		repository.save(customers);
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
	public void findOneByIdAqlPotentialNameClashTest() {
		repository.save(customers);
		final Optional<Customer> retrieved = repository.findOneByIdAqlPotentialNameClash(john.getId());
		assertEquals(john, retrieved.get());
	}

	@Test(expected = IllegalArgumentException.class)
	public void findOneByIdAqlParamNameClashTest() {
		repository.save(customers);
		final ArangoCursor<Customer> retrieved = repository.findOneByIdAqlParamNameClash(john.getId(), john.getName());
		assertEquals(john, retrieved.next());
	}

	@Test
	public void findOneByBindVarsAqlTest() {
		repository.save(customers);
		final Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("id", john.getId());
		bindVars.put("name", john.getName());
		final ArangoCursor<Customer> retrieved = repository.findOneByBindVarsAql(OPTIONS.ttl(127).cache(true),
			bindVars);
		assertEquals(john, retrieved.next());
	}

	@Test(expected = ClassCastException.class)
	public void findOneByBindVarsOfIllegalTypeAqlTest() {
		repository.save(customers);
		final Map<Integer, Object> bindVars = new HashMap<>();
		bindVars.put(1, john.getId());
		bindVars.put(2, john.getName());
		final ArangoCursor<Customer> retrieved = repository.findOneByBindVarsAql(OPTIONS, bindVars);
		assertEquals(john, retrieved.next());
	}

	@Test
	public void findOneByComplementingNameAndBindVarsAqlTest() {
		repository.save(customers);
		final Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("id", john.getId());
		final Customer retrieved = repository.findOneByNameAndBindVarsAql(john.getName(), bindVars);
		assertEquals(john, retrieved);
	}

	@Test
	public void findOneByOverridingNameAndBindVarsAqlTest() {
		repository.save(customers);
		final Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("id", john.getId());
		bindVars.put("name", john.getId() + "random");
		final Customer retrieved = repository.findOneByNameAndBindVarsAql(john.getName(), bindVars);
		assertEquals(john, retrieved);
	}

	@Test(expected = IllegalArgumentException.class)
	public void findOneByBindVarsAndClashingParametersAqlTest() {
		repository.save(customers);
		final Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("id", john.getId());
		bindVars.put("name", john.getName());
		final Customer retrieved = repository.findOneByBindVarsAndClashingParametersAql(bindVars, john.getName(),
			OPTIONS, john.getName());
		assertEquals(john, retrieved);
	}

	@Test(expected = IllegalArgumentException.class)
	public void findOneByNameWithDuplicateOptionsAqlTest() {
		repository.save(customers);
		final Customer retrieved = repository.findOneByNameWithDuplicateOptionsAql(john.getName(), OPTIONS, OPTIONS);
		assertEquals(john, retrieved);
	}

	@Test
	public void findOneByIdAndNameWithBindVarsAqlTest() {
		repository.save(customers);
		final Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("id", john.getId());
		bindVars.put("0", john.getName() + "random");
		final Customer retrieved = repository.findOneByIdAndNameWithBindVarsAql(john.getName(), bindVars);
		assertEquals(john, retrieved);
	}

	@Test
	public void findOneByIdInCollectionAqlTest() {
		repository.save(customers);
		final Customer retrieved = repository.findOneByIdInCollectionAql(john.getId().split("/")[0], john.getId(),
			john.getId());
		assertEquals(john, retrieved);
	}

	@Test
	public void findOneByIdInNamedCollectionAqlTest() {
		repository.save(customers);
		final Customer retrieved = repository.findOneByIdInNamedCollectionAql(john.getId().split("/")[0], john.getId(),
			john.getId());
		assertEquals(john, retrieved);
	}

	@Test(expected = ArangoDBException.class)
	public void findOneByIdInIncorrectNamedCollectionAqlTest() {
		repository.save(customers);
		final Customer retrieved = repository.findOneByIdInIncorrectNamedCollectionAql(john.getId().split("/")[0],
			john.getId(), john.getId());
		assertEquals(john, retrieved);
	}

	@Test(expected = ArangoDBException.class)
	public void findOneByIdInNamedCollectionAqlRejectedTest() {
		repository.save(customers);
		final Customer retrieved = repository.findOneByIdInNamedCollectionAqlRejected(john.getId().split("/")[0],
			john.getId());
		assertEquals(john, retrieved);
	}

	@Test
	public void findManyBySurnameTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("James", "Smith", 35));
		toBeRetrieved.add(new Customer("Matt", "Smith", 34));
		repository.save(toBeRetrieved);
		final List<Customer> retrieved = repository.findManyBySurname("Smith");
		assertTrue(equals(retrieved, toBeRetrieved, cmp, eq, false));

	}
}
