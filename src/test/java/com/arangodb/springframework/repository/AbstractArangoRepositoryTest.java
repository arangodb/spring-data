package com.arangodb.springframework.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.GeoResult;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.testdata.Address;
import com.arangodb.springframework.testdata.Contains;
import com.arangodb.springframework.testdata.Customer;
import com.arangodb.springframework.testdata.Material;
import com.arangodb.springframework.testdata.Owns;
import com.arangodb.springframework.testdata.Product;
import com.arangodb.springframework.testdata.ShoppingCart;

/**
 * Created by F625633 on 12/07/2017.
 */
public abstract class AbstractArangoRepositoryTest extends AbstractArangoTest {

	@Autowired
	protected CustomerRepository repository;

	protected Customer john;
	protected Customer bob;
	protected Set<Customer> customers;
	protected Set<String> ids;
	protected Comparator<Object> cmp = Comparator.comparing(o -> ((Customer) o).getId());
	protected BiPredicate<Object, Object> eq = (a, b) -> cmp.compare(a, b) == 0;
	@SuppressWarnings("unchecked")
	protected Comparator<Object> geoCmp = Comparator.comparing(o -> ((GeoResult<Customer>) o).getContent().getId());
	protected BiPredicate<Object, Object> geoEq = (o1, o2) -> geoCmp.compare(o1, o2) == 0;

	public AbstractArangoRepositoryTest() {
		super(Customer.class, Address.class, ShoppingCart.class, Product.class, Material.class, Contains.class,
				Owns.class);
	}

	@Before
	public void createMockCustomers() {
		john = new Customer("John", "Smith", 20);
		bob = new Customer("Bob", "Thompson", 40);
		customers = new HashSet<>();
		customers.add(john);
		customers.add(bob);
		ids = new HashSet<>();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected boolean equals(
		final Object it1,
		final Object it2,
		final Comparator<Object> cmp,
		final BiPredicate<Object, Object> eq,
		final boolean shouldOrderMatter) {
		final Function<Object, List> iterableToSortedList = it -> {
			final List l = new ArrayList();
			if (it != null) {
				if (it.getClass().isArray()) {
					final Object[] array = (Object[]) it;
					for (final Object e : array) {
						l.add(e);
					}
				} else {
					final Iterable iterable = (Iterable) it;
					for (final Object e : iterable) {
						l.add(e);
					}
				}
			}
			if (!shouldOrderMatter) {
				l.sort(cmp);
			}
			return l;
		};
		final List l1 = iterableToSortedList.apply(it1);
		final List l2 = iterableToSortedList.apply(it2);
		if (l1.size() != l2.size()) {
			return false;
		}
		for (int i = 0; i < l1.size(); ++i) {
			if (!eq.test(l1.get(i), l2.get(i))) {
				return false;
			}
		}
		return true;
	}
}
