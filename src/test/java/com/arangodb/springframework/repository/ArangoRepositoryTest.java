package com.arangodb.springframework.repository;

import com.arangodb.springframework.testdata.Address;
import com.arangodb.springframework.testdata.Customer;
import com.arangodb.springframework.testdata.ShoppingCart;
import com.arangodb.springframework.testdata.UserRecord;
import com.arangodb.springframework.testdata.UserImmutable;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.data.domain.*;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.data.util.Streamable;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by F625633 on 06/07/2017.
 */
public class ArangoRepositoryTest extends AbstractArangoRepositoryTest {

    @Nested
    public class ReturnOriginalEntities {

        @Autowired
        protected CustomerRepository repository;

        @Test
        public void saveTest() {
            Customer res = repository.save(john);
            assertThat(res, sameInstance(john));
            assertThat(res.getId(), is(notNullValue()));
            assertThat(res.getId(), equalTo(john.getId()));
        }

        @Test
        public void saveAllTest() {
            Iterable<Customer> res = repository.saveAll(customers);
            Iterator<Customer> cIt = customers.iterator();
            for (Customer re : res) {
                assertThat(re, sameInstance(cIt.next()));
            }
            Iterable<Customer> docs = repository.findAll();
            docs.forEach(d -> d.setName("saveAllTest"));
            repository.saveAll(docs);
            repository.findAll().forEach(it -> assertThat("name does not match", it.getName(), equalTo("saveAllTest")));
        }
    }

    @Nested
    @TestPropertySource(properties = "returnOriginalEntities=false")
    public class ReturnServerResult {

        @Autowired
        protected CustomerRepository repository;

        @Test
        public void saveTest() {
            Customer res = repository.save(john);
            assertThat(res, not(sameInstance(john)));
            assertThat(res.getId(), is(notNullValue()));
            assertThat(res.getId(), equalTo(john.getId()));
        }

        @Test
        public void saveAllTest() {
            List<Customer> res = Streamable.of(repository.saveAll(customers)).toList();
            assertThat(res, Matchers.hasSize(customers.size()));
            assertThat(res, Matchers.hasItems(customers.toArray(Customer[]::new)));
            for (Customer original : customers) {
                assertThat(res, everyItem(not(sameInstance(original))));
            }
            Iterable<Customer> docs = repository.findAll();
            docs.forEach(d -> d.setName("saveAllTest"));
            repository.saveAll(docs);
            repository.findAll().forEach(it -> assertThat("name does not match", it.getName(), equalTo("saveAllTest")));
        }
    }

    @Nested
    @TestPropertySource(properties = "returnOriginalEntities=false")
    public class JavaRecords {

        @Autowired
        protected UserRecordRepository repository;

        @Test
        public void saveTest() {
            UserRecord i = new UserRecord("mike", 22);
            UserRecord o = repository.save(i);
            assertThat(o, not(sameInstance(i)));
            assertThat(o.key(), is(notNullValue()));
            assertThat(o.id(), is(notNullValue()));
            assertThat(o.rev(), is(notNullValue()));
            assertThat(o.name(), equalTo(i.name()));
            assertThat(o.age(), equalTo(i.age()));
        }

        @Test
        public void saveAllTest() {
            List<UserRecord> i = List.of(
                    new UserRecord("mike", 22),
                    new UserRecord("mary", 33)
            );
            List<UserRecord> o = Streamable.of(repository.saveAll(i)).toList();
            assertThat(o, Matchers.hasSize(i.size()));

            assertThat(o.get(0).key(), is(notNullValue()));
            assertThat(o.get(0).id(), is(notNullValue()));
            assertThat(o.get(0).rev(), is(notNullValue()));
            assertThat(o.get(0).name(), equalTo(i.get(0).name()));
            assertThat(o.get(0).age(), equalTo(i.get(0).age()));

            assertThat(o.get(1).key(), is(notNullValue()));
            assertThat(o.get(1).id(), is(notNullValue()));
            assertThat(o.get(1).rev(), is(notNullValue()));
            assertThat(o.get(1).name(), equalTo(i.get(1).name()));
            assertThat(o.get(1).age(), equalTo(i.get(1).age()));
        }
    }

    @Nested
    @TestPropertySource(properties = "returnOriginalEntities=false")
    public class ImmutableEntity {

        @Autowired
        protected UserImmutableRepository repository;

        @Test
        public void saveTest() {
            UserImmutable i = new UserImmutable("mike", 22);
            UserImmutable o = repository.save(i);
            assertThat(o, not(sameInstance(i)));
            assertThat(o.getKey(), is(notNullValue()));
            assertThat(o.getId(), is(notNullValue()));
            assertThat(o.getRev(), is(notNullValue()));
            assertThat(o.getName(), equalTo(i.getName()));
            assertThat(o.getAge(), equalTo(i.getAge()));
        }

        @Test
        public void saveAllTest() {
            List<UserImmutable> i = List.of(
                    new UserImmutable("mike", 22),
                    new UserImmutable("mary", 33)
            );
            List<UserImmutable> o = Streamable.of(repository.saveAll(i)).toList();
            assertThat(o, Matchers.hasSize(i.size()));

            assertThat(o.get(0).getKey(), is(notNullValue()));
            assertThat(o.get(0).getId(), is(notNullValue()));
            assertThat(o.get(0).getRev(), is(notNullValue()));
            assertThat(o.get(0).getName(), equalTo(i.get(0).getName()));
            assertThat(o.get(0).getAge(), equalTo(i.get(0).getAge()));

            assertThat(o.get(1).getKey(), is(notNullValue()));
            assertThat(o.get(1).getId(), is(notNullValue()));
            assertThat(o.get(1).getRev(), is(notNullValue()));
            assertThat(o.get(1).getName(), equalTo(i.get(1).getName()));
            assertThat(o.get(1).getAge(), equalTo(i.get(1).getAge()));
        }
    }

    @Test
    public void findOneTest() {
        repository.save(john);
        final String id = john.getId();
        Customer customer = repository.findById(id).get();
        assertThat("customers do not match", customer, equalTo(john));
        john.setAge(100);
        repository.save(john);
        customer = repository.findById(id).get();
        assertThat("customers do not match", customer, equalTo(john));
    }

    @Test
    public void findAllByIterableTest() {
        repository.saveAll(customers);
        ids.add(john.getId());
        ids.add(bob.getId());
        final Iterable<Customer> response = repository.findAllById(ids);
        assertThat("customers do not match", equals(customers, response, cmp, eq, false), equalTo(true));
    }

    @Test
    public void findAllTest() {
        repository.saveAll(customers);
        final Iterable<Customer> response = repository.findAll();
        assertThat("customers do not match", equals(customers, response, cmp, eq, false), equalTo(true));
    }

    @Test
    public void countTest() {
        repository.saveAll(customers);
        final long size = repository.count();
        assertThat("customer set sizes do not match", customers.size() == size);
    }

    @Test
    public void existsTest() {
        repository.save(john);
        assertThat("customer does not exist but should", repository.existsById(john.getId()), equalTo(true));
        assertThat("customer exists but should not", repository.existsById(john.getId() + "0"), equalTo(false));
    }

    @Test
    public void deleteByEntityTest() {
        repository.saveAll(customers);
        final String johnId = john.getId();
        repository.delete(john);
        assertThat(repository.existsById(bob.getId()), equalTo(true));
        assertThat(repository.existsById(johnId), equalTo(false));

        assertThrows(IllegalArgumentException.class, () -> repository.delete(null));
    }

    @Test
    public void deleteNonExistingTest() {
        repository.saveAll(customers);
        repository.delete(john);
        assertThrows(OptimisticLockingFailureException.class, () -> repository.delete(john));
    }

    @Test
    public void deleteWithRevTest() {
        repository.saveAll(customers);
        var johnCopy = repository.findById(john.getId()).get();
        johnCopy.setName("Johnny");
        repository.save(johnCopy);
        assertThrows(OptimisticLockingFailureException.class, () -> repository.delete(john));
    }

    @Test
    public void deleteByIdTest() {
        repository.saveAll(customers);
        final String johnId = john.getId();
        repository.deleteById(johnId);
        assertThat(repository.existsById(bob.getId()), equalTo(true));
        assertThat(repository.existsById(johnId), equalTo(false));

        // ensure it does not throw in case entities are not found
        repository.deleteById(johnId);

        assertThrows(IllegalArgumentException.class, () -> repository.deleteById(null));

        // document key  is not valid.
        assertThrows(UncategorizedDataAccessException.class, () -> repository.deleteById(""));
    }

    @Test
    public void deleteAllByIdTest() {
        repository.saveAll(customers);
        final String johnId = john.getId();
        final String bobId = bob.getId();
        repository.deleteAllById(Arrays.asList(johnId, bobId));
        assertThat(repository.existsById(bobId), equalTo(false));
        assertThat(repository.existsById(johnId), equalTo(false));

        // ensure it does not throw in case entities are not found
        repository.deleteAllById(Arrays.asList(johnId, bobId));

        assertThrows(IllegalArgumentException.class, () -> repository.deleteAllById(null));
        assertThrows(IllegalArgumentException.class, () -> repository.deleteAllById(Collections.singleton(null)));

        // document key  is not valid.
        assertThrows(UncategorizedDataAccessException.class, () -> repository.deleteAllById(List.of("")));
    }

    @Test
    public void deleteByIterableTest() {
        repository.saveAll(customers);
        final List<Customer> toDelete = new ArrayList<>();
        toDelete.add(john);
        final String johnId = john.getId();
        repository.deleteAll(toDelete);
        assertThat(repository.existsById(bob.getId()), equalTo(true));
        assertThat(repository.existsById(johnId), equalTo(false));
    }

    @Test
    public void deleteByIterableNonExistingTest() {
        repository.saveAll(customers);
        repository.delete(john);
        assertThrows(OptimisticLockingFailureException.class, () -> repository.deleteAll(List.of(john, bob)));
    }

    @Test
    public void deleteByIterableWithRevTest() {
        repository.saveAll(customers);
        var johnCopy = repository.findById(john.getId()).get();
        johnCopy.setName("Johnny");
        repository.save(johnCopy);
        assertThrows(OptimisticLockingFailureException.class, () -> repository.deleteAll(List.of(john, bob)));
    }

    @Test
    public void deleteAllTest() {
        repository.saveAll(customers);
        repository.deleteAll();
        assertThat(repository.count(), equalTo(0L));
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
        assertThat(equals(toBeRetrieved, retrieved, cmp, eq, true), equalTo(true));
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
        assertThat(retrievedPage.getTotalElements(), equalTo((long) toBeRetrieved.size()));
        assertThat(retrievedPage.getNumber(), equalTo(pageNumber));
        assertThat(retrievedPage.getSize(), equalTo(pageSize));
        assertThat(retrievedPage.getTotalPages(), equalTo((toBeRetrieved.size() + pageSize - 1) / pageSize));
        final List<Customer> expected = toBeRetrieved.subList(pageNumber * pageSize, (pageNumber + 1) * pageSize);
        assertThat(equals(expected, retrievedPage, cmp, eq, true), equalTo(true));
    }

    @Test
    public void findOneByExampleTest() {
        repository.save(john);
        final Customer customer = repository.findOne(Example.of(john)).get();
        assertThat("customers do not match", customer, equalTo(john));
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
        assertThat(equals(checkList, retrievedList, cmp, eq, false), equalTo(true));
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
                ExampleMatcher.GenericPropertyMatcher::regex);
        final Example<Customer> example = Example.of(find, exampleMatcher);
        final Iterable<?> retrieved = repository.findAll(example);
        final List<Customer> retrievedList = new LinkedList<>();
        retrieved.forEach(e -> retrievedList.add((Customer) e));
        final List<Customer> checkList = new LinkedList<>();
        checkList.add(check1);
        checkList.add(check2);
        assertThat(equals(checkList, retrievedList, cmp, eq, false), equalTo(true));
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
                .withIgnoreNullValues().withIgnorePaths("location", "alive"));
        final List<Sort.Order> orders = new LinkedList<>();
        orders.add(new Sort.Order(Sort.Direction.ASC, "name"));
        orders.add(new Sort.Order(Sort.Direction.ASC, "surname"));
        final Sort sort = Sort.by(orders);
        final Iterable<Customer> retrieved = repository.findAll(example, sort);
        assertThat(equals(toBeRetrieved, retrieved, cmp, eq, true), equalTo(true));
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
                .withIgnoreNullValues().withIgnorePaths("location", "alive"));
        final List<Sort.Order> orders = new LinkedList<>();
        orders.add(new Sort.Order(Sort.Direction.ASC, "name"));
        orders.add(new Sort.Order(Sort.Direction.ASC, "surname"));
        final Sort sort = Sort.by(orders);
        final int pageNumber = 1;
        final int pageSize = 3;
        final Page<Customer> retrievedPage = repository.findAll(example, PageRequest.of(pageNumber, pageSize, sort));
        assertThat(retrievedPage.getTotalElements(), equalTo((long) toBeRetrieved.size()));
        assertThat(retrievedPage.getNumber(), equalTo(pageNumber));
        assertThat(retrievedPage.getSize(), equalTo(pageSize));
        assertThat(retrievedPage.getTotalPages(), equalTo((toBeRetrieved.size() + pageSize - 1) / pageSize));
        final List<Customer> expected = toBeRetrieved.subList(pageNumber * pageSize, (pageNumber + 1) * pageSize);
        assertThat(equals(expected, retrievedPage, cmp, eq, true), equalTo(true));
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
                .withIgnoreNullValues().withIgnorePaths("location", "alive"));
        final long size = repository.count(example);
        assertThat(size == toBeRetrieved.size(), equalTo(true));
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
        assertThat(exists, equalTo(true));
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
                .withMatcher("surname", ExampleMatcher.GenericPropertyMatcher::startsWith).withIgnoreCase("surname").withIgnoreNullValues());
        final Customer retrieved = repository.findOne(example).get();
        assertThat(retrieved, equalTo(check));
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
        assertThat(retrieved, equalTo(check));
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
                .withIgnoreNullValues().withIgnorePaths("location", "alive", "age"));
        final Customer retrieved = repository.findOne(example).get();
        assertThat(retrieved, equalTo(check));
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
                .withIgnoreNullValues().withIgnorePaths("location", "alive", "age"));
        final Customer retrieved = repository.findOne(example).get();
        assertThat(retrieved, equalTo(check));
    }

    @Test
    public void findAllByExampleWhitArrayStringTest() {
        final List<Customer> toBeRetrieved = new LinkedList<>();
        final Customer customer1 = new Customer("AbbaXP", "BbaaaaXZ", 1001);
        final Customer customer2 = new Customer("Bwa?[a.b]baAaGH", "", 67);
        customer1.setStringList(Arrays.asList("testA", "testB", "testC"));
        customer2.setStringList(Arrays.asList("test1", "test2", "test3"));
        toBeRetrieved.add(customer1);
        toBeRetrieved.add(customer2);
        repository.saveAll(toBeRetrieved);
        final Customer exampleCustomer = new Customer("AbbaXP", "BbaaaaXZ", 1001);
        exampleCustomer.setStringList(Arrays.asList("testB"));
        final Example<Customer> example = Example.of(exampleCustomer);
        final Customer retrieved = repository.findOne(example).get();
        assertThat(retrieved, equalTo(customer1));
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
                ExampleMatcher.matching().withMatcher("nestedCustomer.name", ExampleMatcher.GenericPropertyMatcher::endsWith)
                        .withIgnoreCase("nestedCustomer.name").withIgnoreNullValues().withTransformer(
                                "nestedCustomer.age", o -> Optional.of(Integer.parseInt(o.get().toString()) + 1)));
        final Customer retrieved = repository.findOne(example).get();
        assertThat(retrieved, equalTo(check));
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
                ExampleMatcher.matching().withMatcher("nestedCustomer.name", ExampleMatcher.GenericPropertyMatcher::endsWith)
                        .withIgnorePaths("arangoId", "id", "key", "rev")
                        .withIgnoreCase("nestedCustomer.name").withIncludeNullValues().withTransformer(
                                "nestedCustomer.age", o -> Optional.of(Integer.parseInt(o.get().toString()) + 1)));
        final Customer retrieved = repository.findOne(example).get();
        assertThat(retrieved, equalTo(check));
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
        assertThat(retrieved, equalTo(customer));
    }

    @Test
    public void findAllUnpagedPageableTest() {
        final List<Customer> toBeRetrieved = new LinkedList<>();
        toBeRetrieved.add(new Customer("Dhiren", "Upadhyay", 30));
        toBeRetrieved.add(new Customer("Ashim", "Upadhyay", 28));
        toBeRetrieved.add(new Customer("Lokendr", "Upadhyay", 24));
        repository.saveAll(toBeRetrieved);
        final Page<Customer> retrievedPage = repository.findAll(Pageable.unpaged());
        assertThat(retrievedPage.getTotalElements(), equalTo((long) toBeRetrieved.size()));
        assertThat(retrievedPage.getSize(), equalTo(toBeRetrieved.size()));
        assertThat(retrievedPage.getContent(), equalTo(toBeRetrieved));
    }

}
