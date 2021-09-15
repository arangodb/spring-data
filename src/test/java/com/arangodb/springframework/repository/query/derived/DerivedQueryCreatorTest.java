package com.arangodb.springframework.repository.query.derived;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.arangodb.springframework.testdata.*;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Range.Bound;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoPage;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;

import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.repository.AbstractArangoRepositoryTest;
import com.arangodb.springframework.repository.query.derived.geo.Ring;

/**
 * Created by N675515 on 27/07/2017.
 */
public class DerivedQueryCreatorTest extends AbstractArangoRepositoryTest {

	@Test
	public void findDistinctTest() {
		repository.saveAll(customers);
		repository.save(new Customer("Bill", "", 0));
		repository.save(new Customer("Alfred", "", 0));
		final Set<Customer> retrieved = repository.findDistinctByNameAfter("Bill");
		assertTrue(equals(customers, retrieved, cmp, eq, false));
	}

	@Test
	public void findOrderTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("B", "", 17));
		toBeRetrieved.add(new Customer("A", "", 0));
		repository.saveAll(toBeRetrieved);
		repository.save(new Customer("A", "", 18));
		repository.save(new Customer("C", "", 0));
		final List<Customer> retrieved = repository.findByNameNotIgnoreCaseAndAgeLessThanIgnoreCaseOrderByNameDesc("c",
			18);
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
	}

	@Test
	public void findTopOrderTest() {
		final String[] stringArray = { "b", "Cf", "aaA" };
		final List<Integer> integerList = new LinkedList<>();
		integerList.add(666);
		final int[] ages = { 7, 11, 19 };
		final String[] names = { "cTa", "Rfh", "JwA" };
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("JwA", "", 11);
		customer1.setStringArray(stringArray);
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("zzz", "", 19);
		customer2.setIntegerList(integerList);
		toBeRetrieved.add(customer2);
		final Customer customer3 = new Customer("cTa", "", 19);
		customer3.setStringArray(stringArray);
		toBeRetrieved.add(customer3);
		repository.saveAll(toBeRetrieved);
		final Customer customer4 = new Customer("AAA", "", 19);
		customer4.setIntegerList(integerList);
		repository.save(customer4);
		final Customer customer5 = new Customer("CtA", "", 6);
		customer5.setStringArray(stringArray);
		repository.save(customer5);
		final Customer customer6 = new Customer("rFH", "", 7);
		customer6.setIntegerList(integerList);
		repository.save(customer6);
		final Iterable<Customer> retrieved = repository
				.findTop3ByAgeInAndStringArrayIgnoreCaseOrNameNotInAndIntegerListIgnoreCaseOrderByAgeAscNameDescAllIgnoreCase(
					ages, new String[] { "B", "cF", "AAa" }, names, integerList);
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
	}

	@Test
	public void findDistinctOrderTest() {
		final String[] stringArray = { "b", "Cf", "aaA" };
		final List<Integer> integerList = new LinkedList<>();
		integerList.add(666);
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("A", "", 99));
		final Customer customer1 = new Customer("dt", "", 0);
		customer1.setStringArray(stringArray);
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("dt", "", 1);
		customer2.setIntegerList(integerList);
		toBeRetrieved.add(customer2);
		repository.saveAll(toBeRetrieved);
		repository.save(new Customer("aaa", "", 17));
		final Customer customer3 = new Customer("DTHB", "", 17);
		customer3.setStringArray(stringArray);
		repository.save(customer3);
		final Customer[] retrieved = repository
				.findDistinctByAgeGreaterThanEqualOrStringArrayAndNameBeforeOrIntegerListOrderByNameAscAgeAscAllIgnoreCase(
					18, new String[] { "B", "cF", "AAa" }, "dThB", integerList);
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
	}

	@Test
	public void findTopDistinctTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("A", "", 0);
		final List<Integer> integerList = new LinkedList<>();
		integerList.add(99);
		customer1.setIntegerList(integerList);
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("B", "", 0);
		customer2.setStringArray(new String[] { "stRing" });
		toBeRetrieved.add(customer2);
		repository.saveAll(toBeRetrieved);
		final Customer customer3 = new Customer("C", "", 0);
		customer3.setIntegerList(integerList);
		repository.save(customer3);
		repository.save(new Customer("A", "", 0));
		final Collection<Customer> retrieved = repository
				.findTop2DistinctByStringArrayContainingIgnoreCaseOrIntegerListNotNullIgnoreCaseOrderByNameAsc(
					"string");
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
	}

	@Test
	public void NotContainingTest() {
		final Collection<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("A", "B", 55);
		final Customer customer2 = new Customer("C", "D", 44);
		customer1.setStringArray(new String[] { "hello", "greetings" });
		customer2.setStringArray(new String[] { "goodbye", "see you later" });
		toBeRetrieved.add(customer2);
		repository.save(customer1);
		repository.save(customer2);
		final Collection<Customer> retrieved = repository.findByStringArrayNotContainingIgnoreCase("Hello");
		assertTrue(equals(retrieved, toBeRetrieved, cmp, eq, false));
	}

	@Test
	public void findByStringContaining() {
		final Customer c1 = new Customer("abc", "", 0);
		final Customer c2 = new Customer("Abc", "", 0);
		final Customer c3 = new Customer("abcd", "", 0);
		final Customer c4 = new Customer("ab", "", 0);
		repository.save(c1);
		repository.save(c2);
		repository.save(c3);
		repository.save(c4);
		final Collection<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(c1);
		toBeRetrieved.add(c3);
		final Collection<Customer> retrieved = repository.findByNameContaining("abc");
		assertTrue(equals(retrieved, toBeRetrieved, cmp, eq, false));
	}

	@Test
	public void findByStringContainingIgnoreCase() {
		final Customer c1 = new Customer("abc", "", 0);
		final Customer c2 = new Customer("Abc", "", 0);
		final Customer c3 = new Customer("abcd", "", 0);
		final Customer c4 = new Customer("ab", "", 0);
		repository.save(c1);
		repository.save(c2);
		repository.save(c3);
		repository.save(c4);
		final Collection<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(c1);
		toBeRetrieved.add(c2);
		toBeRetrieved.add(c3);
		final Collection<Customer> retrieved = repository.findByNameContainingIgnoreCase("abc");
		assertTrue(equals(retrieved, toBeRetrieved, cmp, eq, false));
	}

	@Test
	public void findTest() {
		final Customer customer1 = new Customer("%_\\name", "%surname%", 20);
		repository.save(customer1);
		repository.save(new Customer("_\\name", "%surname%", 20));
		repository.save(new Customer("%_\\name", "%surname", 20));
		repository.save(new Customer("%_\\name", "surname%", 18));
		final Customer retrieved = repository.findByNameStartsWithAndSurnameEndsWithAndAgeBetween("%_\\", "%", 19, 20);
		assertEquals(customer1, retrieved);
	}

	@Test
	public void countTest() {
		final List<Integer> integerList = new LinkedList<>();
		integerList.add(99);
		integerList.add(999);
		repository.save(new Customer("", "", 19));
		final Customer customer = new Customer("", "", 17);
		customer.setIntegerList(integerList);
		repository.save(customer);
		repository.save(new Customer("", "", 18));
		final int retrieved = repository.countByAgeGreaterThanOrStringArrayNullAndIntegerList(18, integerList);
		assertEquals(2, retrieved);
	}

	@Test
	public void countDistinctTest() {
		repository.save(new Customer("", "", 100, true));
		repository.save(new Customer("aqwertyb^\\c_", "", 0, false));
		repository.save(new Customer("", "", 18, false));
		repository.save(new Customer("qwertyb^\\c_", "", 19, false));
		repository.save(new Customer("aqwertyb\\c_", "", 20, false));
		repository.save(new Customer("aqwertyb^c_", "", 21, false));
		final int retrieved = repository.countDistinctByAliveTrueOrNameLikeOrAgeLessThanEqual("a%b_\\\\c\\_", 18);
		assertEquals(3, retrieved);
	}

	@Test
	public void removeTest() {
		final List<Customer> customers = new LinkedList<>();
		repository.save(new Customer("", "", 0, false));
		repository.save(new Customer("ab", "abb---", 0, true));
		customers.add(new Customer("a^b", "abb---", 0, true));
		customers.add(new Customer("", "", 0, true));
		repository.saveAll(customers);
		repository.removeByNameNotLikeAndSurnameRegexOrAliveFalse("%a_b%", "^a[bc]+");
		final Iterable<Customer> retrieved = repository.findAll();
		assertTrue(equals(customers, retrieved, cmp, eq, false));
	}

	@Test
	public void findNearTest() {
		john.setLocation(new int[] { 2, 2 });
		bob.setLocation(new int[] { 50, 45 });
		repository.saveAll(customers);
		final Customer[] retrieved = repository.findByLocationNear(new Point(51, 46));
		final Customer[] check = { bob, john };
		assertTrue(equals(check, retrieved, cmp, eq, true));
	}

	@Test
	public void findNearGeoJsonTest() {
		john.setPosition(new Point(2, 2));
		bob.setPosition(new Point(45, 50));
		repository.saveAll(customers);
		final Customer[] retrieved = repository.findByPositionNear(new Point(51, 46));
		final Customer[] check = { bob, john };
		assertTrue(equals(check, retrieved, cmp, eq, true));
	}

	@Test
	public void findWithinTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("", "", 0);
		customer1.setLocation(new int[] { 11, 0 });
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("", "", 0);
		customer2.setLocation(new int[] { 10, 10 });
		toBeRetrieved.add(customer2);
		final Customer customer3 = new Customer("", "", 0);
		customer3.setLocation(new int[] { 0, 50 });
		toBeRetrieved.add(customer3);
		repository.saveAll(toBeRetrieved);
		final Customer customer4 = new Customer("", "", 0);
		customer4.setLocation(new int[] { 0, 0 });
		repository.save(customer4);
		final Customer customer5 = new Customer("---", "", 0);
		customer5.setLocation(new int[] { 10, 10 });
		repository.save(customer5);
		final Bound<Double> lowerBound = Bound.inclusive(convertAngleToDistance(10));
		final Bound<Double> upperBound = Bound.inclusive(convertAngleToDistance(50));
		final List<Customer> retrieved = repository.findByLocationWithinAndName(new Point(0.11, 0.11),
				Range.of(lowerBound, upperBound), "");
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void findWithinGeoJsonTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("", "", 0);
		customer1.setPosition(new Point( 0, 11 ));
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("", "", 0);
		customer2.setPosition(new Point( 10, 10 ));
		toBeRetrieved.add(customer2);
		final Customer customer3 = new Customer("", "", 0);
		customer3.setPosition(new Point( 50, 0 ));
		toBeRetrieved.add(customer3);
		repository.saveAll(toBeRetrieved);
		final Customer customer4 = new Customer("", "", 0);
		customer4.setPosition(new Point( 0, 0 ));
		repository.save(customer4);
		final Customer customer5 = new Customer("---", "", 0);
		customer5.setPosition(new Point( 10, 10 ));
		repository.save(customer5);
		final Bound<Double> lowerBound = Bound.inclusive(convertAngleToDistance(10));
		final Bound<Double> upperBound = Bound.inclusive(convertAngleToDistance(50));
		final List<Customer> retrieved = repository.findByPositionWithinAndName(new Point(0.11, 0.11),
				Range.of(lowerBound, upperBound), "");
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void findWithinOrNearTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("---", "", 0);
		customer1.setLocation(new int[] { 45, 2 });
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("+++", "", 0);
		customer2.setLocation(new int[] { 60, 1 });
		toBeRetrieved.add(customer2);
		repository.saveAll(toBeRetrieved);
		final Customer customer3 = new Customer("---", "", 0);
		customer3.setLocation(new int[] { 0, 180 });
		repository.save(customer3);
		final double distanceInMeters = convertAngleToDistance(30);
		final Distance distance = new Distance(distanceInMeters / 1000, Metrics.KILOMETERS);
		final Circle circle = new Circle(new Point(0, 20), distance);
		final Iterable<Customer> retrieved = repository.findByLocationWithinOrNameAndLocationNear(circle, "+++",
			new Point(0, 0));
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void geoJsonFindWithinOrNearTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("---", "", 0);
		customer1.setPosition(new Point( 2, 45 ));
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("+++", "", 0);
		customer2.setPosition(new Point( 1, 60 ));
		toBeRetrieved.add(customer2);
		repository.saveAll(toBeRetrieved);
		final Customer customer3 = new Customer("---", "", 0);
		customer3.setPosition(new Point( 180, 0 ));
		repository.save(customer3);
		final double distanceInMeters = convertAngleToDistance(30);
		final Distance distance = new Distance(distanceInMeters / 1000, Metrics.KILOMETERS);
		final Circle circle = new Circle(new Point(0, 20), distance);
		final Iterable<Customer> retrieved = repository.findByPositionWithinOrNameAndPositionNear(circle, "+++",
				new Point(0, 0));
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void findByLocationWithinBoxTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("", "", 0);
		customer1.setLocation(new int[] { 10, 10 });
		repository.save(customer1);
		final Customer customer2 = new Customer("", "", 0);
		customer2.setLocation(new int[] { 0, 0 });
		repository.save(customer2);
		final Customer customer3 = new Customer("", "", 0);
		customer3.setLocation(new int[] { 0, 10 });
		repository.save(customer3);
		final Customer customer4 = new Customer("", "", 0);
		customer4.setLocation(new int[] { 0, 20 });
		repository.save(customer4);
		final Customer customer5 = new Customer("", "", 0);
		customer5.setLocation(new int[] { 10, 0 });
		repository.save(customer5);
		final Customer customer6 = new Customer("", "", 0);
		customer6.setLocation(new int[] { 10, 20 });
		repository.save(customer6);
		final Customer customer7 = new Customer("", "", 0);
		customer7.setLocation(new int[] { 20, 0 });
		repository.save(customer7);
		final Customer customer8 = new Customer("", "", 0);
		customer8.setLocation(new int[] { 20, 10 });
		repository.save(customer8);
		final Customer customer9 = new Customer("", "", 0);
		customer9.setLocation(new int[] { 20, 20 });
		repository.save(customer9);

		toBeRetrieved.add(customer3);
		final List<Customer> retrieved = repository.findByLocationWithin(new Box(new Point(9, -1), new Point(11, 1)));
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void findByGeoJsonPositionWithinBoxTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("", "", 0);
		customer1.setPosition(new Point( 10, 10 ));
		repository.save(customer1);
		final Customer customer2 = new Customer("", "", 0);
		customer2.setPosition(new Point( 0, 0 ));
		repository.save(customer2);
		final Customer customer3 = new Customer("", "", 0);
		customer3.setPosition(new Point( 10, 0 ));
		repository.save(customer3);
		final Customer customer4 = new Customer("", "", 0);
		customer4.setPosition(new Point( 20, 0 ));
		repository.save(customer4);
		final Customer customer5 = new Customer("", "", 0);
		customer5.setPosition(new Point( 0, 10 ));
		repository.save(customer5);
		final Customer customer6 = new Customer("", "", 0);
		customer6.setPosition(new Point( 20, 10 ));
		repository.save(customer6);
		final Customer customer7 = new Customer("", "", 0);
		customer7.setPosition(new Point( 0, 20 ));
		repository.save(customer7);
		final Customer customer8 = new Customer("", "", 0);
		customer8.setPosition(new Point( 10, 20 ));
		repository.save(customer8);
		final Customer customer9 = new Customer("", "", 0);
		customer9.setPosition(new Point( 20, 20 ));
		repository.save(customer9);

		toBeRetrieved.add(customer3);
		final List<Customer> retrieved = repository.findByPositionWithin(new Box(new Point(9, -1), new Point(11, 1)));
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void findWithinAndWithinTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("+++", "", 0);
		customer1.setLocation(new int[] { 80, 0 });
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("vvv", "", 0);
		customer2.setLocation(new int[] { 10, 0 });
		toBeRetrieved.add(customer2);
		repository.saveAll(toBeRetrieved);
		final Customer customer3 = new Customer("--d", "", 0);
		customer3.setLocation(new int[] { 19, 0 });
		repository.save(customer3);
		final Customer customer4 = new Customer("--r", "", 0);
		customer4.setLocation(new int[] { 6, 0 });
		repository.save(customer4);
		final Customer customer5 = new Customer("-!r", "", 0);
		customer5.setLocation(new int[] { 0, 0 });
		repository.save(customer5);
		final int distance = (int) convertAngleToDistance(11);
		final Bound<Integer> lowerBound = Bound.inclusive((int) convertAngleToDistance(5));
		final Bound<Integer> upperBound = Bound.inclusive((int) convertAngleToDistance(15));
		final Collection<Customer> retrieved = repository.findByLocationWithinAndLocationWithinOrName(new Point(0, 20),
			distance, new Ring<>(new Point(0, 0), Range.of(lowerBound, upperBound)), "+++");
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void geoJsonFindWithinAndWithinTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("+++", "", 0);
		customer1.setPosition(new Point ( 0, 80 ));
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("vvv", "", 0);
		customer2.setPosition(new Point ( 0, 10 ));
		toBeRetrieved.add(customer2);
		repository.saveAll(toBeRetrieved);
		final Customer customer3 = new Customer("--d", "", 0);
		customer3.setPosition(new Point ( 0, 19 ));
		repository.save(customer3);
		final Customer customer4 = new Customer("--r", "", 0);
		customer4.setPosition(new Point ( 0, 6 ));
		repository.save(customer4);
		final Customer customer5 = new Customer("-!r", "", 0);
		customer5.setPosition(new Point ( 0, 0 ));
		repository.save(customer5);
		final int distance = (int) convertAngleToDistance(11);
		final Bound<Integer> lowerBound = Bound.inclusive((int) convertAngleToDistance(5));
		final Bound<Integer> upperBound = Bound.inclusive((int) convertAngleToDistance(15));
		final Collection<Customer> retrieved = repository.findByPositionWithinAndPositionWithinOrName(new Point(0, 20),
				distance, new Ring<>(new Point(0, 0), Range.of(lowerBound, upperBound)), "+++");
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void findByMultipleLocationsAndMultipleRegularFieldsTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("John", "", 0);
		customer1.setLocation(new int[] { 89, 0 });
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("Bob", "", 0);
		customer2.setLocation(new int[] { 0, 5 });
		toBeRetrieved.add(customer2);
		final Customer customer3 = new Customer("Peter", "Pen", 0);
		customer3.setLocation(new int[] { 0, 89 });
		toBeRetrieved.add(customer3);
		final Customer customer4 = new Customer("Jack", "Sparrow", 0);
		customer4.setLocation(new int[] { 70, 20 });
		toBeRetrieved.add(customer4);
		repository.saveAll(toBeRetrieved);
		final Customer customer5 = new Customer("Peter", "The Great", 0);
		customer5.setLocation(new int[] { 0, 89 });
		repository.save(customer5);
		final Customer customer6 = new Customer("Ballpoint", "Pen", 0);
		customer6.setLocation(new int[] { 0, 89 });
		repository.save(customer6);
		final Customer customer7 = new Customer("Jack", "Reacher", 0);
		customer7.setLocation(new int[] { 70, 20 });
		repository.save(customer7);
		final Customer customer8 = new Customer("Jack", "Sparrow", 0);
		customer8.setLocation(new int[] { 15, 65 });
		repository.save(customer8);
		final Customer customer9 = new Customer("Jack", "Sparrow", 0);
		customer9.setLocation(new int[] { 25, 75 });
		repository.save(customer9);
		final double distance = convertAngleToDistance(10);
		final Bound<Double> lowerBound = Bound.inclusive(convertAngleToDistance(10));
		final Bound<Double> upperBound = Bound.inclusive(convertAngleToDistance(20));
		final Range<Double> distanceRange = Range.of(lowerBound, upperBound);
		final List<Customer> retrieved = repository
				.findByNameOrLocationWithinOrNameAndSurnameOrNameAndLocationNearAndSurnameAndLocationWithin("John",
					new Point(0, 0), distance, "Peter", "Pen", "Jack", new Point(47, 63), "Sparrow", new Point(10, 60),
					distanceRange);
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void geoJsonFindByMultipleLocationsAndMultipleRegularFieldsTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("John", "", 0);
		customer1.setPosition(new Point ( 0, 89 ));
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("Bob", "", 0);
		customer2.setPosition(new Point ( 5, 0 ));
		toBeRetrieved.add(customer2);
		final Customer customer3 = new Customer("Peter", "Pen", 0);
		customer3.setPosition(new Point ( 89, 0 ));
		toBeRetrieved.add(customer3);
		final Customer customer4 = new Customer("Jack", "Sparrow", 0);
		customer4.setPosition(new Point ( 20, 70 ));
		toBeRetrieved.add(customer4);
		repository.saveAll(toBeRetrieved);
		final Customer customer5 = new Customer("Peter", "The Great", 0);
		customer5.setPosition(new Point ( 89, 0 ));
		repository.save(customer5);
		final Customer customer6 = new Customer("Ballpoint", "Pen", 0);
		customer6.setPosition(new Point ( 89, 0 ));
		repository.save(customer6);
		final Customer customer7 = new Customer("Jack", "Reacher", 0);
		customer7.setPosition(new Point ( 20, 70 ));
		repository.save(customer7);
		final Customer customer8 = new Customer("Jack", "Sparrow", 0);
		customer8.setPosition(new Point ( 65, 15 ));
		repository.save(customer8);
		final Customer customer9 = new Customer("Jack", "Sparrow", 0);
		customer9.setPosition(new Point ( 75, 25 ));
		repository.save(customer9);
		final double distance = convertAngleToDistance(10);
		final Bound<Double> lowerBound = Bound.inclusive(convertAngleToDistance(10));
		final Bound<Double> upperBound = Bound.inclusive(convertAngleToDistance(20));
		final Range<Double> distanceRange = Range.of(lowerBound, upperBound);
		final List<Customer> retrieved = repository
				.findByNameOrPositionWithinOrNameAndSurnameOrNameAndPositionNearAndSurnameAndPositionWithin("John",
						new Point(0, 0), distance, "Peter", "Pen", "Jack", new Point(47, 63), "Sparrow", new Point(10, 60),
						distanceRange);
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void existsAndStringListAllIgnoreCaseTest() {
		final List<String> stringList = new LinkedList<>();
		stringList.add("qweRty");
		final List<String> stringList2 = new LinkedList<>();
		stringList2.add("qwerty");
		final List<String> stringList3 = new LinkedList<>();
		stringList3.add("qwety");
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("", "", 0);
		customer1.setStringList(stringList);
		customer1.setNestedCustomer(new Customer("nested", "", 0));
		toBeRetrieved.add(customer1);
		repository.saveAll(toBeRetrieved);
		final Customer customer2 = new Customer("", "", 0);
		customer2.setStringList(stringList3);
		repository.save(customer2);
		template.insert(new IncompleteCustomer("Incomplete", stringList2));
		final Customer[] retrieved = repository.findByNestedCustomerAliveExistsAndStringListAllIgnoreCase(stringList2);
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void staticAndDynamicSortTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(new Customer("Tony", "1", 5));
		toBeRetrieved.add(new Customer("Tony", "2", 3));
		toBeRetrieved.add(new Customer("Tony", "2", 4));
		repository.saveAll(toBeRetrieved);
		repository.save(new Customer("Stark", "0", 0));
		final Customer[] retrieved = repository.findByNameOrderBySurnameAsc(Sort.by("age"), "Tony");
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
	}

	@Test
	public void pageableTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		repository.save(new Customer("", "", 0));
		repository.save(new Customer("", "", 1));
		toBeRetrieved.add(new Customer("", "", 2));
		repository.save(new Customer("-", "", 3));
		toBeRetrieved.add(new Customer("", "", 4));
		repository.save(new Customer("", "", 5));
		repository.saveAll(toBeRetrieved);
		final Pageable pageable = PageRequest.of(1, 2, Sort.by("age"));
		final Page<Customer> retrieved = repository.readByNameAndSurname(pageable, "",
			new AqlQueryOptions().fullCount(false), "");
		assertEquals(5, retrieved.getTotalElements());
		assertEquals(3, retrieved.getTotalPages());
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
	}

	@Test
	public void geoResultTest() {
		final Customer customer1 = new Customer("", "", 0);
		customer1.setLocation(new int[] { 7, 5 });
		repository.save(customer1);
		final Customer customer2 = new Customer("", "", 0);
		customer2.setLocation(new int[] { 70, 50 });
		repository.save(customer2);
		final double distance = convertAngleToDistance(10);
		final GeoResult<Customer> retrieved = repository.queryByLocationWithin(new Point(1, 2), distance);
		final double expectedDistanceInMeters = getDistanceBetweenPoints(new Point(5, 7), new Point(1, 2));
		final double expectedNormalizedDistance = expectedDistanceInMeters / 1000.0
				/ Metrics.KILOMETERS.getMultiplier();
		assertEquals(customer1, retrieved.getContent());
		assertEquals(expectedNormalizedDistance, retrieved.getDistance().getNormalizedValue(), 0.000000001);
	}

	@Test
	public void geoJsonGeoResultTest() {
		final Customer customer1 = new Customer("", "", 0);
		customer1.setPosition(new Point( 5, 7 ));
		repository.save(customer1);
		final Customer customer2 = new Customer("", "", 0);
		customer2.setPosition(new Point( 50, 70 ));
		repository.save(customer2);
		final double distance = convertAngleToDistance(10);
		final GeoResult<Customer> retrieved = repository.queryByPositionWithin(new Point(1, 2), distance);
		final double expectedDistanceInMeters = getDistanceBetweenPoints(new Point(5, 7), new Point(1, 2));
		final double expectedNormalizedDistance = expectedDistanceInMeters / 1000.0
				/ Metrics.KILOMETERS.getMultiplier();
		assertEquals(customer1, retrieved.getContent());
		assertEquals(expectedNormalizedDistance, retrieved.getDistance().getNormalizedValue(), 0.000000001);
	}

	@Test
	public void geoResultsTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("", "", 0);
		customer1.setLocation(new int[] { 43, 21 });
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("", "", 0);
		customer2.setLocation(new int[] { 21, 43 });
		toBeRetrieved.add(customer2);
		repository.saveAll(toBeRetrieved);
		final Customer customer3 = new Customer("", "", 0);
		customer3.setLocation(new int[] { 70, 50 });
		repository.save(customer3);
		final Customer customer4 = new Customer("", "", 0);
		customer4.setLocation(new int[] { 3, 2 });
		repository.save(customer4);
		final Bound<Double> lowerBound = Bound.inclusive(convertAngleToDistance(30));
		final Bound<Double> upperBound = Bound.inclusive(convertAngleToDistance(50));
		final GeoResults<Customer> retrieved = repository.findByLocationWithin(new Point(1, 0),
			Range.of(lowerBound, upperBound));
		final List<GeoResult<Customer>> expectedGeoResults = new LinkedList<>();
		expectedGeoResults.add(new GeoResult<>(customer1,
				new Distance(getDistanceBetweenPoints(new Point(1, 0), new Point(21, 43)) / 1000, Metrics.KILOMETERS)));
		expectedGeoResults.add(new GeoResult<>(customer2,
				new Distance(getDistanceBetweenPoints(new Point(1, 0), new Point(43, 21)) / 1000, Metrics.KILOMETERS)));
		assertTrue(equals(expectedGeoResults, retrieved, geoCmp, geoEq, false));
	}

	@Test
	public void geoPageTest() {
		final Customer customer1 = new Customer("", "", 0);
		customer1.setLocation(new int[] { 2, 0 });
		repository.save(customer1);
		final Customer customer2 = new Customer("", "", 0);
		customer2.setLocation(new int[] { 3, 0 });
		repository.save(customer2);
		final Customer customer3 = new Customer("", "", 0);
		customer3.setLocation(new int[] { 4, 0 });
		repository.save(customer3);
		final Customer customer4 = new Customer("", "", 0);
		customer4.setLocation(new int[] { 6, 0 });
		repository.save(customer4);
		final GeoPage<Customer> retrieved = repository.findByLocationNear(new Point(0, 0), PageRequest.of(1, 2));
		final List<GeoResult<Customer>> expectedGeoResults = new LinkedList<>();
		expectedGeoResults.add(new GeoResult<>(customer3,
				new Distance(getDistanceBetweenPoints(new Point(0, 0), new Point(0, 4)) / 1000, Metrics.KILOMETERS)));
		expectedGeoResults.add(new GeoResult<>(customer4,
				new Distance(getDistanceBetweenPoints(new Point(0, 0), new Point(0, 6)) / 1000, Metrics.KILOMETERS)));
		assertEquals(4, retrieved.getTotalElements());
		assertEquals(2, retrieved.getTotalPages());
		assertTrue(equals(expectedGeoResults, retrieved, geoCmp, geoEq, true));
	}

	@Test
	public void buildPredicateWithDistanceTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("+", "", 0);
		customer1.setLocation(new int[] { 89, 89 });
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("", "+", 0);
		customer2.setLocation(new int[] { 5, 0 });
		toBeRetrieved.add(customer2);
		final Customer customer3 = new Customer("", "", 0);
		customer3.setLocation(new int[] { 0, 25 });
		toBeRetrieved.add(customer3);
		repository.saveAll(toBeRetrieved);
		final Customer customer4 = new Customer("", "", 0);
		customer4.setLocation(new int[] { 15, 0 });
		repository.save(customer4);
		final Customer customer5 = new Customer("", "", 0);
		customer5.setLocation(new int[] { 0, 35 });
		repository.save(customer5);
		final double distanceInMeters = convertAngleToDistance(10);
		final Distance distance = new Distance(distanceInMeters / 1000, Metrics.KILOMETERS);
		final Range<Distance> distanceRange = Range.of(
			Bound.inclusive(new Distance(convertAngleToDistance(20) / 1000, Metrics.KILOMETERS)),
			Bound.inclusive(new Distance(convertAngleToDistance(30) / 1000, Metrics.KILOMETERS)));
		final Point location = new Point(0, 0);
		final GeoResults<Customer> retrieved = repository.findByNameOrSurnameAndLocationWithinOrLocationWithin("+", "+",
			location, distance, location, distanceRange);
		final List<GeoResult<Customer>> expectedGeoResults = new LinkedList<>();
		expectedGeoResults.add(new GeoResult<>(customer1,
				new Distance(getDistanceBetweenPoints(location, new Point(89, 89)) / 1000, Metrics.KILOMETERS)));
		expectedGeoResults.add(new GeoResult<>(customer2,
				new Distance(getDistanceBetweenPoints(location, new Point(0, 5)) / 1000, Metrics.KILOMETERS)));
		expectedGeoResults.add(new GeoResult<>(customer3,
				new Distance(getDistanceBetweenPoints(location, new Point(25, 0)) / 1000, Metrics.KILOMETERS)));
		assertTrue(equals(expectedGeoResults, retrieved, geoCmp, geoEq, false));
	}

	@Test
	public void existsTest() {
		repository.save(john);
		assertTrue(repository.existsByName("John"));
		assertTrue(!repository.existsByName("Bob"));
	}

	@Test
	public void polygonTest() {
		final int[][] locations = { { 11, 31 }, { 20, 20 }, { 20, 40 }, { 70, 30 }, { 40, 10 }, { -10, -10 },
				{ -10, 20 }, { -10, 60 }, { 30, 50 }, { 10, 20 }, { 5, 30 } };
		final Customer[] customers = new Customer[11];
		final List<Customer> toBeRetrieved = new LinkedList<>();
		for (int i = 0; i < customers.length; ++i) {
			customers[i] = new Customer("", "", 0);
			customers[i].setLocation(locations[i]);
			repository.save(customers[i]);
			if (i < 3) {
				toBeRetrieved.add(customers[i]);
			}
		}
		final Polygon polygon = new Polygon(new Point(0, 0), new Point(30, 60), new Point(50, 0), new Point(30, 10),
				new Point(30, 20));
		final List<Customer> retrieved = repository.findByLocationWithin(polygon);
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void geoJsonPolygonTest() {
		final int[][] locations = { { 11, 31 }, { 20, 20 }, { 20, 40 }, { 70, 30 }, { 40, 10 }, { -10, -10 },
				{ -10, 20 }, { -10, 60 }, { 30, 50 }, { 10, 20 }, { 5, 30 } };
		final Customer[] customers = new Customer[11];
		final List<Customer> toBeRetrieved = new LinkedList<>();
		for (int i = 0; i < customers.length; ++i) {
			customers[i] = new Customer("", "", 0);
			customers[i].setPosition(new Point(locations[i][1], locations[i][0]));
			repository.save(customers[i]);
			if (i < 3) {
				toBeRetrieved.add(customers[i]);
			}
		}
		final Polygon polygon = new Polygon(new Point(0, 0), new Point(30, 60), new Point(50, 0), new Point(30, 10),
				new Point(30, 20), new Point(0, 0));
		final List<Customer> retrieved = repository.findByPositionWithin(polygon);
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void nestedPropertiesTest() {
		john.setNestedCustomer(bob);
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(john);
		repository.saveAll(toBeRetrieved);
		repository.save(bob);
		final List<Customer> retrieved = repository.findByNestedCustomerName("Bob");
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void notPersistentNestedPropertyTest() {
		john.setNickname(new Nickname("Johnny"));
		final List<Customer> toBeRetrieved = new LinkedList<>();
		toBeRetrieved.add(john);
		repository.saveAll(toBeRetrieved);
		repository.save(bob);
		final List<Customer> retrieved = repository.findByNicknameValue("Johnny");
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void referenceTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("", "", 0);
		final Customer customer11 = new Customer("", "", 0);
		final ShoppingCart cart11 = new ShoppingCart();
		final Product product1 = new Product("asdf1");
		template.insert(product1);
		final Product product2 = new Product("2");
		template.insert(product2);
		cart11.setProducts(Arrays.asList(product1, product2));
		template.insert(cart11);
		customer11.setShoppingCart(cart11);
		template.insert(customer11);
		final Customer customer12 = new Customer("", "", 0);
		final ShoppingCart cart12 = new ShoppingCart();
		final Product product3 = new Product("3");
		template.insert(product3);
		final Product product4 = new Product("4");
		template.insert(product4);
		cart12.setProducts(Arrays.asList(product3, product4));
		template.insert(cart12);
		customer12.setShoppingCart(cart12);
		template.insert(customer12);
		final Customer nested1 = new Customer("", "", 0);
		nested1.setNestedCustomer(customer11);
		template.insert(nested1);
		final Customer nested2 = new Customer("", "", 0);
		nested2.setNestedCustomer(customer12);
		template.insert(nested2);
		customer1.setNestedCustomers(Arrays.asList(nested1, nested2));
		repository.save(customer1);
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("", "", 0);
		final Customer customer21 = new Customer("", "", 0);
		final ShoppingCart cart21 = new ShoppingCart();
		cart21.setProducts(Arrays.asList(product2, product3, product4));
		template.insert(cart21);
		final Customer nested3 = new Customer("", "", 0);
		nested3.setNestedCustomer(customer21);
		template.insert(nested3);
		customer2.setNestedCustomers(Arrays.asList(nested2, nested3));
		repository.save(customer2);
		repository.saveAll(toBeRetrieved);
		final List<Customer> retrieved = repository
				.findByNestedCustomersNestedCustomerShoppingCartProductsNameLike("%1%");
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	@Ignore // https://github.com/arangodb/arangodb/issues/5303
	public void referenceGeospatialTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final Customer customer1 = new Customer("", "", 0);
		final Customer customer11 = new Customer("", "", 0);
		final ShoppingCart cart11 = new ShoppingCart();
		final Product product1 = new Product("asdf1", new double[] { 10, 12 });
		template.insert(product1);
		final Product product2 = new Product("2", new double[] { 30, 42 });
		template.insert(product2);
		cart11.setProducts(Arrays.asList(product1, product2));
		template.insert(cart11);
		customer11.setShoppingCart(cart11);
		template.insert(customer11);
		final Customer customer12 = new Customer("", "", 0);
		final ShoppingCart cart12 = new ShoppingCart();
		final Product product3 = new Product("3", new double[] { 40, 62 });
		template.insert(product3);
		final Product product4 = new Product("4", new double[] { 50, 52 });
		template.insert(product4);
		cart12.setProducts(Arrays.asList(product3, product4));
		template.insert(cart12);
		customer12.setShoppingCart(cart12);
		template.insert(customer12);
		final Customer nested1 = new Customer("", "", 0);
		nested1.setNestedCustomer(customer11);
		template.insert(nested1);
		final Customer nested2 = new Customer("", "", 0);
		nested2.setNestedCustomer(customer12);
		template.insert(nested2);
		customer1.setNestedCustomers(Arrays.asList(nested1, nested2));
		repository.save(customer1);
		toBeRetrieved.add(customer1);
		final Customer customer2 = new Customer("", "", 0);
		final Customer customer21 = new Customer("", "", 0);
		final ShoppingCart cart21 = new ShoppingCart();
		cart21.setProducts(Arrays.asList(product2, product3, product4));
		template.insert(cart21);
		final Customer nested3 = new Customer("", "", 0);
		nested3.setNestedCustomer(customer21);
		template.insert(nested3);
		customer2.setNestedCustomers(Arrays.asList(nested2, nested3));
		repository.save(customer2);
		repository.saveAll(toBeRetrieved);
		final List<Customer> retrieved = repository
				.findByNestedCustomersNestedCustomerShoppingCartProductsLocationWithin(new Point(1, 2),
					convertAngleToDistance(25));
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void relationsSingleLevelTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final List<Customer> customers = new LinkedList<>();
		List<Customer> retrieved;
		final Customer john = new Customer("John", "Smith", 52);
		final Customer adam = new Customer("Adam", "Smith", 294);
		final Customer matt = new Customer("Matt", "Smith", 34);
		final Product phone = new Product("phone");
		final Product car = new Product("car");
		final Product chair = new Product("chair");
		template.insert(phone);
		template.insert(car);
		template.insert(chair);
		customers.add(john);
		customers.add(matt);
		customers.add(adam);
		repository.saveAll(customers);
		template.insert(new Owns(john, phone));
		template.insert(new Owns(john, car));
		template.insert(new Owns(adam, chair));
		template.insert(new Owns(matt, phone));
		template.insert(new Owns(matt, car));
		template.insert(new Owns(matt, chair));
		toBeRetrieved.add(john);
		toBeRetrieved.add(matt);
		retrieved = repository.getByOwnsName(phone.getName());
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void twoRelationsSingleLevelTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final List<Customer> customers = new LinkedList<>();
		List<Customer> retrieved;
		final Customer john = new Customer("John", "Smith", 52);
		final Customer adam = new Customer("Adam", "Smith", 294);
		final Customer matt = new Customer("Matt", "Smith", 34);
		final Product phone = new Product("phone");
		final Product car = new Product("car");
		final Product chair = new Product("chair");
		template.insert(phone);
		template.insert(car);
		template.insert(chair);
		customers.add(john);
		customers.add(matt);
		customers.add(adam);
		repository.saveAll(customers);
		template.insert(new Owns(john, phone));
		template.insert(new Owns(john, car));
		template.insert(new Owns(adam, chair));
		template.insert(new Owns(matt, phone));
		template.insert(new Owns(matt, car));
		template.insert(new Owns(matt, chair));
		toBeRetrieved.add(john);
		toBeRetrieved.add(matt);
		retrieved = repository.getByOwnsNameAndOwns2Name(phone.getName(), phone.getName());
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	@Test
	public void relationsMultiLevelTest() {
		final List<Customer> toBeRetrieved = new LinkedList<>();
		final List<Customer> customers = new LinkedList<>();
		List<Customer> retrieved;
		final Customer john = new Customer("John", "Smith", 52);
		final Customer adam = new Customer("Adam", "Smith", 294);
		final Customer matt = new Customer("Matt", "Smith", 34);
		final Product phone = new Product("phone");
		final Product car = new Product("car");
		final Product chair = new Product("chair");
		template.insert(phone);
		template.insert(car);
		template.insert(chair);
		final Material wood = new Material("wood");
		final Material metal = new Material("metal");
		final Material glass = new Material("glass");
		template.insert(wood);
		template.insert(metal);
		template.insert(glass);
		customers.add(john);
		customers.add(matt);
		customers.add(adam);
		repository.saveAll(customers);
		template.insert(new Owns(john, phone));
		template.insert(new Owns(john, car));
		template.insert(new Owns(adam, chair));
		template.insert(new Owns(matt, phone));
		template.insert(new Owns(matt, car));
		template.insert(new Owns(matt, chair));
		template.insert(new Contains(phone, glass));
		template.insert(new Contains(car, metal));
		template.insert(new Contains(car, glass));
		template.insert(new Contains(chair, wood));
		toBeRetrieved.add(adam);
		toBeRetrieved.add(matt);
		retrieved = repository.getByOwnsContainsName(wood.getName());
		assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
	}

	private double convertAngleToDistance(final int angle) {
		final int EARTH_RADIUS = 6371000;
		return 2 * Math.PI * EARTH_RADIUS * (angle / 360.0);
	}

	private double getDistanceBetweenPoints(final Point point1, final Point point2) {
		final String query = String.format(Locale.ENGLISH, "RETURN DISTANCE(%f, %f, %f, %f)", point1.getY(),
			point1.getX(), point2.getY(), point2.getX());
		return template.query(query, new HashMap<>(), null, Double.class).next();
	}
}
