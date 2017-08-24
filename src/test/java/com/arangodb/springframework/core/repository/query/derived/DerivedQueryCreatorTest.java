package com.arangodb.springframework.core.repository.query.derived;

import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.core.repository.AbstractArangoRepositoryTest;

import com.arangodb.springframework.core.repository.query.derived.geo.Ring;
import com.arangodb.springframework.testdata.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.domain.*;
import org.springframework.data.geo.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by N675515 on 27/07/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class DerivedQueryCreatorTest extends AbstractArangoRepositoryTest {

    @Test
    public void findDistinctTest() {
        repository.save(customers);
        repository.save(new Customer("Bill", "", 0));
        repository.save(new Customer("Alfred", "", 0));
        Set<Customer> retrieved = repository.findDistinctByNameAfter("Bill");
        assertTrue(equals(customers, retrieved, cmp, eq, false));
    }

    @Test
    public void findOrderTest() {
        List<Customer> toBeRetrieved = new LinkedList<>();
        toBeRetrieved.add(new Customer("B", "", 17));
        toBeRetrieved.add(new Customer("A", "", 0));
        repository.save(toBeRetrieved);
        repository.save(new Customer("A", "", 18));
        repository.save(new Customer("C", "", 0));
        List<Customer> retrieved = repository.findByNameNotIgnoreCaseAndAgeLessThanIgnoreCaseOrderByNameDesc("c", 18);
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
    }

    @Test
    public void findTopOrderTest() {
        String[] stringArray = {"b", "Cf", "aaA"};
        List<Integer> integerList = new LinkedList<>();
        integerList.add(666);
        int[] ages = {7, 11, 19};
        String[] names = {"cTa", "Rfh", "JwA"};
        List<Customer> toBeRetrieved = new LinkedList<>();
        Customer customer1 = new Customer("JwA", "", 11);
        customer1.setStringArray(stringArray);
        toBeRetrieved.add(customer1);
        Customer customer2 = new Customer("zzz", "", 19);
        customer2.setIntegerList(integerList);
        toBeRetrieved.add(customer2);
        Customer customer3 = new Customer("cTa", "", 19);
        customer3.setStringArray(stringArray);
        toBeRetrieved.add(customer3);
        repository.save(toBeRetrieved);
        Customer customer4 = new Customer("AAA", "", 19);
        customer4.setIntegerList(integerList);
        repository.save(customer4);
        Customer customer5 = new Customer("CtA", "", 6);
        customer5.setStringArray(stringArray);
        repository.save(customer5);
        Customer customer6 = new Customer("rFH", "", 7);
        customer6.setIntegerList(integerList);
        repository.save(customer6);
        Iterable<Customer> retrieved = repository.findTop3ByAgeInAndStringArrayIgnoreCaseOrNameNotInAndIntegerListIgnoreCaseOrderByAgeAscNameDescAllIgnoreCase(ages, new String[]  {"B", "cF", "AAa"}, names, integerList);
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
    }

    @Test
    public void findDistinctOrderTest() {
        String[] stringArray = {"b", "Cf", "aaA"};
        List<Integer> integerList = new LinkedList<>();
        integerList.add(666);
        List<Customer> toBeRetrieved = new LinkedList<>();
        toBeRetrieved.add(new Customer("A", "", 99));
        Customer customer1 = new Customer("dt", "", 0);
        customer1.setStringArray(stringArray);
        toBeRetrieved.add(customer1);
        Customer customer2 = new Customer("dt", "", 1);
        customer2.setIntegerList(integerList);
        toBeRetrieved.add(customer2);
        repository.save(toBeRetrieved);
        repository.save(new Customer("aaa", "", 17));
        Customer customer3 = new Customer("DTHB", "", 17);
        customer3.setStringArray(stringArray);
        repository.save(customer3);
        Customer[] retrieved = repository.findDistinctByAgeGreaterThanEqualOrStringArrayAndNameBeforeOrIntegerListOrderByNameAscAgeAscAllIgnoreCase(18, new String[]  {"B", "cF", "AAa"}, "dThB", integerList);
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
    }

    @Test
    public void findTopDistinctTest() {
        List<Customer> toBeRetrieved = new LinkedList<>();
        Customer customer1 = new Customer("A", "", 0);
        List<Integer> integerList = new LinkedList<>();
        integerList.add(99);
        customer1.setIntegerList(integerList);
        toBeRetrieved.add(customer1);
        Customer customer2 = new Customer("B", "", 0);
        customer2.setStringArray(new String[] {"stRing"});
        toBeRetrieved.add(customer2);
        repository.save(toBeRetrieved);
        Customer customer3 = new Customer("C", "", 0);
        customer3.setIntegerList(integerList);
        repository.save(customer3);
        repository.save(new Customer("A", "", 0));
        Collection<Customer> retrieved = repository.findTop2DistinctByStringArrayContainingIgnoreCaseOrIntegerListNotNullIgnoreCaseOrderByNameAsc("string");
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
    }

    @Test
    public void NotContainingTest() {
        Collection<Customer> toBeRetrieved = new LinkedList<>();
        Customer customer1 = new Customer("A", "B", 55);
        Customer customer2 = new Customer("C", "D", 44);
        customer1.setStringArray(new String[] {"hello", "greetings"});
        customer2.setStringArray(new String[] {"goodbye", "see you later"});
        toBeRetrieved.add(customer2);
        repository.save(customer1);
        repository.save(customer2);
        Collection<Customer> retrieved = repository.findByStringArrayNotContainingIgnoreCase("Hello");
        assertTrue(equals(retrieved, toBeRetrieved, cmp, eq, false));
    }

    @Test
    public void findTest() {
        Customer customer1 = new Customer("%_\\name", "%surname%", 20);
        repository.save(customer1);
        repository.save(new Customer("_\\name", "%surname%", 20));
        repository.save(new Customer("%_\\name", "%surname", 20));
        repository.save(new Customer("%_\\name", "surname%", 18));
        Customer retrieved = repository.findByNameStartsWithAndSurnameEndsWithAndAgeBetween("%_\\", "%", 19, 20);
        assertEquals(customer1, retrieved);
    }

    @Test
    public void countTest() {
        List<Integer> integerList = new LinkedList<>();
        integerList.add(99);
        integerList.add(999);
        repository.save(new Customer("", "", 19));
        Customer customer = new Customer("", "", 17);
        customer.setIntegerList(integerList);
        repository.save(customer);
        repository.save(new Customer("", "", 18));
        int retrieved = repository.countByAgeGreaterThanOrStringArrayNullAndIntegerList(18, integerList);
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
        int retrieved = repository.countDistinctByAliveTrueOrNameLikeOrAgeLessThanEqual("a%b_\\\\c\\_", 18);
        assertEquals(3, retrieved);
    }

    @Test
    public void removeTest() {
        List<Customer> customers = new LinkedList<>();
        repository.save(new Customer("", "", 0, false));
        repository.save(new Customer("ab", "abb---", 0, true));
        customers.add(new Customer("a^b", "abb---", 0, true));
        customers.add(new Customer("", "", 0, true));
        repository.save(customers);
        repository.removeByNameNotLikeAndSurnameRegexOrAliveFalse("%a_b%", "^a[bc]+");
        Iterable<Customer> retrieved = repository.findAll();
        assertTrue(equals(customers, retrieved, cmp, eq, false));
    }

    @Test
    public void findNearTest() {
        john.setLocation(new int[] {2, 2});
        bob.setLocation(new int[] {50, 45});
        repository.save(customers);
        Customer[] retrieved = repository.findByLocationNear(new Point(10,20));
        Customer[] check = {john, bob};
        assertTrue(equals(check, retrieved, cmp, eq, true));
    }

    @Test
    public void findWithinTest() {
        List<Customer> toBeRetrieved = new LinkedList<>();
        Customer customer1 = new Customer("", "", 0);
        customer1.setLocation(new int[] {11, 0});
        toBeRetrieved.add(customer1);
        Customer customer2 = new Customer("", "", 0);
        customer2.setLocation(new int[] {10, 10});
        toBeRetrieved.add(customer2);
        Customer customer3 = new Customer("", "", 0);
        customer3.setLocation(new int[] {0, 50});
        toBeRetrieved.add(customer3);
        repository.save(toBeRetrieved);
        Customer customer4 = new Customer("", "", 0);
        customer4.setLocation(new int[] {0, 0});
        repository.save(customer4);
        Customer customer5 = new Customer("---", "", 0);
        customer5.setLocation(new int[] {10, 10});
        repository.save(customer5);
        double lowerBound = convertAngleToDistance(10);
        double upperBound = convertAngleToDistance(50);
        List<Customer> retrieved = repository.findByLocationWithinAndName(new Point(0, 0), new Range<>(lowerBound, upperBound), "");
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
    }

    @Test
    public void findWithinOrNearTest() {
        List<Customer> toBeRetrieved = new LinkedList<>();
        Customer customer1 = new Customer("---", "", 0);
        customer1.setLocation(new int[] {45, 2});
        toBeRetrieved.add(customer1);
        Customer customer2 = new Customer("+++", "", 0);
        customer2.setLocation(new int[] {60, 1});
        toBeRetrieved.add(customer2);
        repository.save(toBeRetrieved);
        Customer customer3 = new Customer("---", "", 0);
        customer3.setLocation(new int[] {0, 180});
        repository.save(customer3);
        double distanceInMeters = convertAngleToDistance(30);
        Distance distance = new Distance(distanceInMeters / 1000, Metrics.KILOMETERS);
        Circle circle = new Circle(new Point(0, 20), distance);
        Iterable<Customer> retrieved = repository.findByLocationWithinOrNameAndLocationNear(circle, "+++", new Point(0, 0));
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
    }

    @Test
    public void findByLocationWithinBoxTest() {
        List<Customer> toBeRetrieved = new LinkedList<>();
        Customer customer1 = new Customer("", "", 0);
        customer1.setLocation(new int[] {10, 10});
        toBeRetrieved.add(customer1);
        repository.save(toBeRetrieved);
        Customer customer2 = new Customer("", "", 0);
        customer2.setLocation(new int[] {0, 0});
        repository.save(customer2);
        Customer customer3 = new Customer("", "", 0);
        customer3.setLocation(new int[] {0, 10});
        repository.save(customer3);
        Customer customer4 = new Customer("", "", 0);
        customer4.setLocation(new int[] {0, 20});
        repository.save(customer4);
        Customer customer5 = new Customer("", "", 0);
        customer5.setLocation(new int[] {10, 0});
        repository.save(customer5);
        Customer customer6 = new Customer("", "", 0);
        customer6.setLocation(new int[] {10, 20});
        repository.save(customer6);
        Customer customer7 = new Customer("", "", 0);
        customer7.setLocation(new int[] {20, 0});
        repository.save(customer7);
        Customer customer8 = new Customer("", "", 0);
        customer8.setLocation(new int[] {20, 10});
        repository.save(customer8);
        Customer customer9 = new Customer("", "", 0);
        customer9.setLocation(new int[] {20, 20});
        repository.save(customer9);
        List<Customer> retrieved = repository.findByLocationWithin(new Box(new Point(5 , 5), new Point(15, 15)));
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
    }

    @Test
    public void findWithinAndWithinTest() {
        List<Customer> toBeRetrieved = new LinkedList<>();
        Customer customer1 = new Customer("+++", "", 0);
        customer1.setLocation(new int[] {80, 0});
        toBeRetrieved.add(customer1);
        Customer customer2 = new Customer("vvv", "", 0);
        customer2.setLocation(new int[] {10, 0});
        toBeRetrieved.add(customer2);
        repository.save(toBeRetrieved);
        Customer customer3 = new Customer("--d", "", 0);
        customer3.setLocation(new int[] {19, 0});
        repository.save(customer3);
        Customer customer4 = new Customer("--r", "", 0);
        customer4.setLocation(new int[] {6, 0});
        repository.save(customer4);
        Customer customer5 = new Customer("-!r", "", 0);
        customer5.setLocation(new int[] {0, 0});
        repository.save(customer5);
        int distance = (int) convertAngleToDistance(11);
        int lowerBound = (int) convertAngleToDistance(5);
        int upperBound = (int) convertAngleToDistance(15);
        Collection<Customer> retrieved = repository.findByLocationWithinAndLocationWithinOrName(new Point(0, 20),
                distance, new Ring(new Point(0, 0), new Range<>(lowerBound, upperBound)), "+++");
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
    }

    @Test
    public void findByMultipleLocationsAndMultipleRegularFieldsTest() {
        List<Customer> toBeRetrieved = new LinkedList<>();
        Customer customer1 = new Customer("John", "", 0);
        customer1.setLocation(new int[] {89, 0});
        toBeRetrieved.add(customer1);
        Customer customer2 = new Customer("Bob", "", 0);
        customer2.setLocation(new int[] {0, 5});
        toBeRetrieved.add(customer2);
        Customer customer3 = new Customer("Peter", "Pen", 0);
        customer3.setLocation(new int[] {0, 89});
        toBeRetrieved.add(customer3);
        Customer customer4 = new Customer("Jack", "Sparrow", 0);
        customer4.setLocation(new int[] {70, 20});
        toBeRetrieved.add(customer4);
        repository.save(toBeRetrieved);
        Customer customer5 = new Customer("Peter", "The Great", 0);
        customer5.setLocation(new int[] {0, 89});
        repository.save(customer5);
        Customer customer6 = new Customer("Ballpoint", "Pen", 0);
        customer6.setLocation(new int[] {0, 89});
        repository.save(customer6);
        Customer customer7 = new Customer("Jack", "Reacher", 0);
        customer7.setLocation(new int[] {70, 20});
        repository.save(customer7);
        Customer customer8 = new Customer("Jack", "Sparrow", 0);
        customer8.setLocation(new int[] {15, 65});
        repository.save(customer8);
        Customer customer9 = new Customer("Jack", "Sparrow", 0);
        customer9.setLocation(new int[] {25, 75});
        repository.save(customer9);
        double distance = convertAngleToDistance(10);
        double lowerBound = convertAngleToDistance(10);
        double upperBound = convertAngleToDistance(20);
        Range<Double> distanceRange = new Range<>(lowerBound, upperBound);
        List<Customer> retrieved = repository.findByNameOrLocationWithinOrNameAndSurnameOrNameAndLocationNearAndSurnameAndLocationWithin(
                "John", new Point(0 ,0), distance, "Peter", "Pen", "Jack", new Point(47, 63), "Sparrow", new Point(10, 60), distanceRange
        );
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
    }

    @Test
    public void existsAndStringListAllIgnoreCaseTest() {
        List<String> stringList = new LinkedList<>();
        stringList.add("qweRty");
        List<String> stringList2 = new LinkedList<>();
        stringList2.add("qwerty");
        List<String> stringList3 = new LinkedList<>();
        stringList3.add("qwety");
        List<Customer> toBeRetrieved = new LinkedList<>();
        Customer customer1 = new Customer("", "", 0);
        customer1.setStringList(stringList);
        customer1.setNestedCustomer(new Customer("nested", "", 0));
        toBeRetrieved.add(customer1);
        repository.save(toBeRetrieved);
        Customer customer2 = new Customer("", "", 0);
        customer2.setStringList(stringList3);
        repository.save(customer2);
        template.insert(new IncompleteCustomer("Incomplete", stringList2));
        Customer[] retrieved = repository.findByNestedCustomerAliveExistsAndStringListAllIgnoreCase(stringList2);
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
    }

    @Test
    public void staticAndDynamicSortTest() {
        List<Customer> toBeRetrieved = new LinkedList<>();
        toBeRetrieved.add(new Customer("Tony", "1", 5));
        toBeRetrieved.add(new Customer("Tony", "2", 3));
        toBeRetrieved.add(new Customer("Tony", "2", 4));
        repository.save(toBeRetrieved);
        repository.save(new Customer("Stark", "0", 0));
        Customer[] retrieved = repository.findByNameOrderBySurnameAsc(new Sort("age"), "Tony");
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
    }

    @Test
    public void pageableTest() {
        List<Customer> toBeRetrieved = new LinkedList<>();
        repository.save(new Customer("", "", 0));
        repository.save(new Customer("", "", 1));
        toBeRetrieved.add(new Customer("", "", 2));
        repository.save(new Customer("-", "", 3));
        toBeRetrieved.add(new Customer("", "", 4));
        repository.save(new Customer("", "", 5));
        repository.save(toBeRetrieved);
        Pageable pageable = new PageRequest(1, 2, new Sort("age"));
        Page<Customer> retrieved = repository.readByNameAndSurname(pageable, "", new AqlQueryOptions().fullCount(false), "");
        assertEquals(5, retrieved.getTotalElements());
        assertEquals(3, retrieved.getTotalPages());
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, true));
    }

    @Test
    public void geoResultTest() {
        Customer customer1 = new Customer("", "", 0);
        customer1.setLocation(new int[] {7, 5});
        repository.save(customer1);
        Customer customer2 = new Customer("", "", 0);
        customer2.setLocation(new int[] {70, 50});
        repository.save(customer2);
        double distance = convertAngleToDistance(10);
        GeoResult<Customer> retrieved = repository.queryByLocationWithin(new Point(1, 2), distance);
        double expectedDistanceInMeters = getDistanceBetweenPoints(new Point(5, 7), new Point(1, 2));
        double expectedNormalizedDistance = expectedDistanceInMeters / 1000.0 / Metrics.KILOMETERS.getMultiplier();
        assertEquals(customer1, retrieved.getContent());
        assertEquals(expectedNormalizedDistance, retrieved.getDistance().getNormalizedValue(), 0.000000001);
    }

    @Test
    public void geoResultsTest() {
        List<Customer> toBeRetrieved = new LinkedList<>();
        Customer customer1 = new Customer("", "", 0);
        customer1.setLocation(new int[] {43, 21});
        toBeRetrieved.add(customer1);
        Customer customer2 = new Customer("", "", 0);
        customer2.setLocation(new int[] {21, 43});
        toBeRetrieved.add(customer2);
        repository.save(toBeRetrieved);
        Customer customer3 = new Customer("", "", 0);
        customer3.setLocation(new int[] {70, 50});
        repository.save(customer3);
        Customer customer4 = new Customer("", "", 0);
        customer4.setLocation(new int[] {3, 2});
        repository.save(customer4);
        double lowerBound = convertAngleToDistance(30);
        double upperBound = convertAngleToDistance(50);
        GeoResults<Customer> retrieved = repository.findByLocationWithin(new Point(1, 0), new Range<>(lowerBound, upperBound));
        List<GeoResult<Customer>> expectedGeoResults = new LinkedList<>();
        expectedGeoResults.add(new GeoResult<>(customer1, new Distance(getDistanceBetweenPoints(new Point(1, 0), new Point(21, 43)) / 1000, Metrics.KILOMETERS)));
        expectedGeoResults.add(new GeoResult<>(customer2, new Distance(getDistanceBetweenPoints(new Point(1, 0), new Point(43, 21)) / 1000, Metrics.KILOMETERS)));
        assertTrue(equals(expectedGeoResults, retrieved, geoCmp, geoEq, false));
    }

    @Test
    public void geoPageTest() {
        Customer customer1 = new Customer("", "", 0);
        customer1.setLocation(new int[] {2, 0});
        repository.save(customer1);
        Customer customer2 = new Customer("", "", 0);
        customer2.setLocation(new int[] {3, 0});
        repository.save(customer2);
        Customer customer3 = new Customer("", "", 0);
        customer3.setLocation(new int[] {4, 0});
        repository.save(customer3);
        Customer customer4 = new Customer("", "", 0);
        customer4.setLocation(new int[] {6, 0});
        repository.save(customer4);
        GeoPage<Customer> retrieved = repository.findByLocationNear(new Point(0, 0), new PageRequest(1, 2));
        List<GeoResult<Customer>> expectedGeoResults = new LinkedList<>();
        expectedGeoResults.add(new GeoResult<>(customer3, new Distance(getDistanceBetweenPoints(new Point(0, 0), new Point(0, 4)) / 1000, Metrics.KILOMETERS)));
        expectedGeoResults.add(new GeoResult<>(customer4, new Distance(getDistanceBetweenPoints(new Point(0, 0), new Point(0, 6)) / 1000, Metrics.KILOMETERS)));
        assertEquals(4, retrieved.getTotalElements());
        assertEquals(2, retrieved.getTotalPages());
        assertTrue(equals(expectedGeoResults, retrieved, geoCmp, geoEq, true));
    }

    @Test
    public void buildPredicateWithDistanceTest() {
        List<Customer> toBeRetrieved = new LinkedList<>();
        Customer customer1 = new Customer("+", "", 0);
        customer1.setLocation(new int[] {89, 89});
        toBeRetrieved.add(customer1);
        Customer customer2 = new Customer("", "+", 0);
        customer2.setLocation(new int[] {5, 0});
        toBeRetrieved.add(customer2);
        Customer customer3 = new Customer("", "", 0);
        customer3.setLocation(new int[] {0, 25});
        toBeRetrieved.add(customer3);
        repository.save(toBeRetrieved);
        Customer customer4 = new Customer("", "", 0);
        customer4.setLocation(new int[] {15, 0});
        repository.save(customer4);
        Customer customer5 = new Customer("", "", 0);
        customer5.setLocation(new int[] {0, 35});
        repository.save(customer5);
        double distanceInMeters = convertAngleToDistance(10);
        Distance distance = new Distance(distanceInMeters / 1000, Metrics.KILOMETERS);
        Range<Distance> distanceRange = new Range<>(
                new Distance(convertAngleToDistance(20) / 1000, Metrics.KILOMETERS),
                new Distance(convertAngleToDistance(30) / 1000, Metrics.KILOMETERS));
        Point location = new Point(0, 0);
        GeoResults<Customer> retrieved = repository.findByNameOrSurnameAndLocationWithinOrLocationWithin("+", "+", location, distance, location, distanceRange);
        List<GeoResult<Customer>> expectedGeoResults = new LinkedList<>();
        expectedGeoResults.add(new GeoResult<>(customer1, new Distance(getDistanceBetweenPoints(location, new Point(89, 89)) / 1000, Metrics.KILOMETERS)));
        expectedGeoResults.add(new GeoResult<>(customer2, new Distance(getDistanceBetweenPoints(location, new Point(0,5)) / 1000, Metrics.KILOMETERS)));
        expectedGeoResults.add(new GeoResult<>(customer3, new Distance(getDistanceBetweenPoints(location, new Point(25, 0)) / 1000, Metrics.KILOMETERS)));
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
        int[][] locations = {
                {11, 31},
                {20, 20},
                {20, 40},
                {70, 30},
                {40, 10},
                {-10, -10},
                {-10, 20},
                {-10, 60},
                {30, 50},
                {10, 20},
                {5, 30}
        };
        Customer[] customers = new Customer[11];
        List<Customer> toBeRetrieved = new LinkedList<>();
        for (int i = 0; i < customers.length; ++i) {
            customers[i] = new Customer("", "", 0);
            customers[i].setLocation(locations[i]);
            repository.save(customers[i]);
            if (i < 3) { toBeRetrieved.add(customers[i]); }
        }
        Polygon polygon = new Polygon(new Point(0, 0), new Point(30, 60), new Point(50, 0), new Point(30, 10), new Point(30, 20));
        List<Customer> retrieved = repository.findByLocationWithin(polygon);
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
    }

    @Test
    public void nestedPropertiesTest() {
        john.setNestedCustomer(bob);
        List<Customer> toBeRetrieved = new LinkedList<>();
        toBeRetrieved.add(john);
        repository.save(toBeRetrieved);
        repository.save(bob);
        List<Customer> retrieved = repository.findByNestedCustomerName("Bob");
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
    }

    @Test
    public void referenceTest() {
        List<Customer> toBeRetrieved = new LinkedList<>();
        Customer customer1 = new Customer("", "", 0);
        Customer customer11 = new Customer("", "", 0);
        ShoppingCart cart11 = new ShoppingCart();
        Product product1 = new Product("asdf1");
        template.insert(product1);
        Product product2 = new Product("2");
        template.insert(product2);
        cart11.setProducts(Arrays.asList(product1, product2));
        template.insert(cart11);
        customer11.setShoppingCart(cart11);
        template.insert(customer11);
        Customer customer12 = new Customer("", "", 0);
        ShoppingCart cart12 = new ShoppingCart();
        Product product3 = new Product("3");
        template.insert(product3);
        Product product4 = new Product("4");
        template.insert(product4);
        cart12.setProducts(Arrays.asList(product3, product4));
        template.insert(cart12);
        customer12.setShoppingCart(cart12);
        template.insert(customer12);
        Customer nested1 = new Customer("", "", 0);
        nested1.setNestedCustomer(customer11);
        template.insert(nested1);
        Customer nested2 = new Customer("", "", 0);
        nested2.setNestedCustomer(customer12);
        template.insert(nested2);
        customer1.setNestedCustomers(Arrays.asList(nested1, nested2));
        repository.save(customer1);
        toBeRetrieved.add(customer1);
        Customer customer2 = new Customer("", "", 0);
        Customer customer21 = new Customer("", "", 0);
        ShoppingCart cart21 = new ShoppingCart();
        cart21.setProducts(Arrays.asList(product2, product3, product4));
        template.insert(cart21);
        Customer nested3 = new Customer("", "", 0);
        nested3.setNestedCustomer(customer21);
        template.insert(nested3);
        customer2.setNestedCustomers(Arrays.asList(nested2, nested3));
        repository.save(customer2);
        repository.save(toBeRetrieved);
        List<Customer> retrieved = repository.findByNestedCustomersNestedCustomerShoppingCartProductsNameLike("%1%");
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
    }

    @Test
    public void referenceGeospatialTest() {
        List<Customer> toBeRetrieved = new LinkedList<>();
        Customer customer1 = new Customer("", "", 0);
        Customer customer11 = new Customer("", "", 0);
        ShoppingCart cart11 = new ShoppingCart();
        Product product1 = new Product("asdf1", new double[] {10, 12});
        template.insert(product1);
        Product product2 = new Product("2", new double[] {30, 42});
        template.insert(product2);
        cart11.setProducts(Arrays.asList(product1, product2));
        template.insert(cart11);
        customer11.setShoppingCart(cart11);
        template.insert(customer11);
        Customer customer12 = new Customer("", "", 0);
        ShoppingCart cart12 = new ShoppingCart();
        Product product3 = new Product("3", new double[] {40, 62});
        template.insert(product3);
        Product product4 = new Product("4", new double[] {50, 52});
        template.insert(product4);
        cart12.setProducts(Arrays.asList(product3, product4));
        template.insert(cart12);
        customer12.setShoppingCart(cart12);
        template.insert(customer12);
        Customer nested1 = new Customer("", "", 0);
        nested1.setNestedCustomer(customer11);
        template.insert(nested1);
        Customer nested2 = new Customer("", "", 0);
        nested2.setNestedCustomer(customer12);
        template.insert(nested2);
        customer1.setNestedCustomers(Arrays.asList(nested1, nested2));
        repository.save(customer1);
        toBeRetrieved.add(customer1);
        Customer customer2 = new Customer("", "", 0);
        Customer customer21 = new Customer("", "", 0);
        ShoppingCart cart21 = new ShoppingCart();
        cart21.setProducts(Arrays.asList(product2, product3, product4));
        template.insert(cart21);
        Customer nested3 = new Customer("", "", 0);
        nested3.setNestedCustomer(customer21);
        template.insert(nested3);
        customer2.setNestedCustomers(Arrays.asList(nested2, nested3));
        repository.save(customer2);
        repository.save(toBeRetrieved);
        List<Customer> retrieved = repository.findByNestedCustomersNestedCustomerShoppingCartProductsLocationWithin(
                new Point(1, 2), convertAngleToDistance(25));
        assertTrue(equals(toBeRetrieved, retrieved, cmp, eq, false));
    }

    @Test
    public void relationsSingleLevelTest() {
        List<Customer> toBeRetrieved = new LinkedList<>();
        List<Customer> customers = new LinkedList<>();
        List<Customer> retrieved;
        Customer john = new Customer("John", "Smith", 52);
        Customer adam = new Customer("Adam", "Smith", 294);
        Customer matt = new Customer("Matt", "Smith", 34);
        Product phone = new Product("phone");
        Product car = new Product("car");
        Product chair = new Product("chair");
        template.insert(phone);
        template.insert(car);
        template.insert(chair);
        customers.add(john);
        customers.add(matt);
        customers.add(adam);
        repository.save(customers);
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

    private double convertAngleToDistance(int angle) {
        final int EARTH_RADIUS = 6371000;
        return 2 * Math.PI * EARTH_RADIUS * (angle / 360.0);
    }

    private double getDistanceBetweenPoints(Point point1, Point point2) {
        String query = String.format("RETURN DISTANCE(%f, %f, %f, %f)", point1.getY(), point1.getX(), point2.getY(), point2.getX());
        return template.query(query, new HashMap<>(), null, Double.class).next();
    }

    private List<Object> execute(String query) {
        return template.query(query, new HashMap<>(), null, Object.class).asListRemaining();
    }
}
