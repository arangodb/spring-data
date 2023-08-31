package com.arangodb.springframework.repository.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsIn.isOneOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.arangodb.springframework.testdata.*;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.BaseDocument;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.repository.AbstractArangoRepositoryTest;
import com.arangodb.springframework.repository.OverriddenCrudMethodsRepository;

/**
 *
 * @author Audrius Malele
 * @author Mark McCormick
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public class ArangoAqlQueryTest extends AbstractArangoRepositoryTest {

	@Autowired
	protected OverriddenCrudMethodsRepository overriddenRepository;

    @Test
    public void dynamicFilter() {
        repository.saveAll(customers);

		Map<String, Object> filters = new HashMap<>();
		filters.put("`customer-name`", "John");
		filters.put("surname", "Smith");
		filters.put("age", 20);
		List<Customer> retrieved = repository.findByAllEqual(filters);
		assertThat(retrieved, hasSize(1));
		assertEquals(john, retrieved.get(0));

		filters.put("age", 21);
		retrieved = repository.findByAllEqual(filters);
		assertThat(retrieved, hasSize(0));
	}

	@Test
	public void findOneByIdAqlWithNamedParameterTest() {
		repository.saveAll(customers);
		final Map<String, Object> retrieved = repository.findOneByIdAqlWithNamedParameter(john.getId(),
			new AqlQueryOptions());
		assertThat(retrieved, hasEntry("_key", john.getId()));
		assertThat(retrieved, hasEntry("_rev", john.getRev()));
		assertThat(retrieved, hasEntry("customer-name", john.getName()));
		assertThat(retrieved, hasEntry("surname", john.getSurname()));
		assertThat(retrieved, hasEntry("age", john.getAge()));
		assertThat(retrieved, hasEntry("alive", john.isAlive()));
	}

	@Test
	public void findOneByIdAndNameAqlTest() {
		repository.saveAll(customers);
		final BaseDocument retrieved = repository.findOneByIdAndNameAql(john.getId(), john.getName());
		assertThat(retrieved.getKey(), is(john.getId()));
		assertThat(retrieved.getRevision(), is(john.getRev()));
		assertThat(retrieved.getAttribute("customer-name"), is(john.getName()));
		assertThat(retrieved.getAttribute("surname"), is(john.getSurname()));
		assertThat(retrieved.getAttribute("age"), is(john.getAge()));
		assertThat(retrieved.getAttribute("alive"), is(john.isAlive()));
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
	public void overrideQueryOptions() {
		Integer countNotOverridden = repository.sequenceTo10K(1, new AqlQueryOptions()).getCount();
		assertThat(countNotOverridden, is(10_000));

		Integer countOverridden = repository.sequenceTo10K(1, new AqlQueryOptions().count(false)).getCount();
		assertThat(countOverridden, nullValue());
	}

	@Test
	public void mergeQueryOptions() {
		List<Double> cursorNotMerged = repository.sequenceTo10K(0, new AqlQueryOptions()).asListRemaining();
		assertThat(cursorNotMerged, hasSize(10_001));
		assertThat(cursorNotMerged.get(0), nullValue());
		assertThrows(ArangoDBException.class,
				()-> repository.sequenceTo10K(0, new AqlQueryOptions().failOnWarning(true)));
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
		bindVars.put("name", john.getName() + "random");
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
	public void findManyBySurnameOnImportedQueryTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("James", "Smith", 35));
		toBeRetrieved.add(new Customer("Matt", "Smith", 34));
		repository.saveAll(toBeRetrieved);
		final List<Customer> retrieved = repository.importedQuery("Smith");
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
		for (final CustomerNameProjection proj : retrieved) {
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
		for (final CustomerNameProjection proj : retrieved) {
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

	@Test
	public void overriddenCrudMethodsTest() {
		overriddenRepository.saveAll(customers);
		final Iterator<Customer> customers = overriddenRepository.findAll().iterator();
		assertThat(customers.hasNext(), is(true));
		assertThat(customers.next(), is(nullValue()));
	}

	@Test
	public void findOneUsingEmbeddedEntities() {
		Material wood = new Material("wood");
		Material glass = new Material("glass");
		template.insert(wood);
		template.insert(glass);

		Product pa = new Product("a");
		Product pb = new Product("b");
		template.insert(Arrays.asList(pa, pb), Product.class);
		template.insert(new Contains(pa, wood));
		template.insert(new Contains(pb, glass));

		Customer owner = new Customer("A", "C", 4);
		template.insert(owner);
		template.insert(new Owns(owner, pa));
		template.insert(new Owns(owner, pb));

		Customer actual = repository.findByIdUsingEmbeddedEntities(owner.getId()).get();
		assertThat(actual.getOwns(), containsInAnyOrder(pa, pb));
		assertThat(actual.getOwns2(), is(actual.getOwns()));

		List<Material> materials = actual.getOwns().stream().map(Product::getContains).collect(Collectors.toList());
		assertThat(materials, hasItems(wood, glass));
	}

	@Test
	public void embeddedEntitiesNull() {
		Product pa = new Product("a");
		Product pb = new Product("b");
		template.insert(Arrays.asList(pa, pb), Product.class);

		Customer owner = new Customer("A", "C", 4);
		template.insert(owner);
		template.insert(new Owns(owner, pa));
		template.insert(new Owns(owner, pb));

		Customer actual = repository.embeddedEntitiesNull(owner.getId()).get();
		assertThat(actual.getOwns(), containsInAnyOrder(pa, pb));
		assertThat(actual.getOwns2(), is(actual.getOwns()));
	}

	@Test
	public void embeddedEntitiesEmptyArray() {
		Customer owner = new Customer("A", "C", 4);
		template.insert(owner);

		Customer actual = repository.embeddedEntitiesEmptyArray(owner.getId()).get();
		assertThat(actual.getOwns().size(), is(0));
	}

}
