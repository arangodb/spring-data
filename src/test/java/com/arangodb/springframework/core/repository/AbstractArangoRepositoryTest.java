package com.arangodb.springframework.core.repository;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.testdata.Customer;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Created by F625633 on 12/07/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public abstract class AbstractArangoRepositoryTest extends AbstractArangoTest {

	@Autowired
	protected CustomerRepository repository;

	protected Customer john;
	protected Customer bob;
	protected Set<Customer> customers;
	protected Set<String> ids;
	protected Comparator<Customer> cmp = (a, b) -> (a.getId().compareTo(b.getId()));
	protected BiPredicate<Customer, Customer> eq = (a, b) -> (a.getId().compareTo(b.getId()) == 0);

	@Before
	public void createMockCustomers() {
		john = new Customer("John", "Smith", 20);
		bob = new Customer("Bob", "Thompson", 40);
		customers = new HashSet<>();
		customers.add(john);
		customers.add(bob);
		ids = new HashSet<>();
	}

	protected <T> boolean equals(Object it1, Object it2, Comparator<T> cmp, BiPredicate<T, T> eq, boolean shouldOrderMatter) {
		Function<Object, List<T>> iterableToSortedList = it -> {
			List<T> l = new ArrayList<>();
			if (it != null) {
				if (it.getClass().isArray()) {
					Object[] array = (Object[]) it;
					for (Object e : array) l.add((T) e);
				} else {
					Iterable<T> iterable = (Iterable<T>) it;
					for (T e : iterable) l.add(e);
				}
			}
			if (!shouldOrderMatter) l.sort(cmp);
			return l;
		};
		List<T> l1 = iterableToSortedList.apply(it1);
		List<T> l2 = iterableToSortedList.apply(it2);
		if (l1.size() != l2.size()) return false;
		for (int i = 0; i < l1.size(); ++i) if (!eq.test(l1.get(i), l2.get(i))) return false;
		return true;
	}
}
