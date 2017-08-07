package com.arangodb.springframework.core.repository;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.testdata.Customer;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.GeoResult;
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
	protected Comparator<Object> cmp = Comparator.comparing(o -> ((Customer) o).getId());
	protected BiPredicate<Object, Object> eq = (a, b) -> cmp.compare(a, b) == 0;
	protected Comparator<Object> geoCmp = Comparator.comparing(o -> ((GeoResult<Customer>) o).getContent().getId());
	protected BiPredicate<Object, Object> geoEq = (o1, o2) -> geoCmp.compare(o1, o2) == 0;

	@Before
	public void createMockCustomers() {
		john = new Customer("John", "Smith", 20);
		bob = new Customer("Bob", "Thompson", 40);
		customers = new HashSet<>();
		customers.add(john);
		customers.add(bob);
		ids = new HashSet<>();
	}

	protected boolean equals(Object it1, Object it2, Comparator<Object> cmp, BiPredicate<Object, Object> eq, boolean shouldOrderMatter) {
		Function<Object, List> iterableToSortedList = it -> {
			List l = new ArrayList();
			if (it != null) {
				if (it.getClass().isArray()) {
					Object[] array = (Object[]) it;
					for (Object e : array) { l.add(e); }
				} else {
					Iterable iterable = (Iterable) it;
					for (Object e : iterable) { l.add(e); }
				}
			}
			if (!shouldOrderMatter) { l.sort(cmp); }
			return l;
		};
		List l1 = iterableToSortedList.apply(it1);
		List l2 = iterableToSortedList.apply(it2);
		if (l1.size() != l2.size()) return false;
		for (int i = 0; i < l1.size(); ++i) if (!eq.test(l1.get(i), l2.get(i))) return false;
		return true;
	}
}
