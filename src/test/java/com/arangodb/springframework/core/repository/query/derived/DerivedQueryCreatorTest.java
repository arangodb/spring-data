package com.arangodb.springframework.core.repository.query.derived;

import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.core.repository.AbstractArangoRepositoryTest;

import com.arangodb.springframework.core.repository.query.derived.geo.Range;
import com.arangodb.springframework.testdata.Customer;
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
        Collection<Customer> retrieved = repository.findByLocationWithinAndLocationWithinOrName(new Point(0, 20), distance, new Point(0, 0), new Range<>(lowerBound, upperBound), "+++");
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
        toBeRetrieved.add(customer1);
        repository.save(toBeRetrieved);
        Customer customer2 = new Customer("", "", 0);
        customer2.setStringList(stringList3);
        repository.save(customer2);
        Customer[] retrieved = repository.findByAliveExistsAndStringListAllIgnoreCase(stringList2);
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
        double distance = convertAngleToDistance(10);
        Range<Double> distanceRange = new Range<>(convertAngleToDistance(20), convertAngleToDistance(30));
        Point location = new Point(0, 0);
        GeoResults<Customer> retrieved = repository.findByNameOrSurnameAndLocationWithinOrLocationWithin("+", "+", location, distance, location, distanceRange);
        List<GeoResult<Customer>> expectedGeoResults = new LinkedList<>();
        expectedGeoResults.add(new GeoResult<>(customer1, new Distance(getDistanceBetweenPoints(location, new Point(89, 89)) / 1000, Metrics.KILOMETERS)));
        expectedGeoResults.add(new GeoResult<>(customer2, new Distance(getDistanceBetweenPoints(location, new Point(0,5)) / 1000, Metrics.KILOMETERS)));
        expectedGeoResults.add(new GeoResult<>(customer3, new Distance(getDistanceBetweenPoints(location, new Point(25, 0)) / 1000, Metrics.KILOMETERS)));
        assertTrue(equals(expectedGeoResults, retrieved, geoCmp, geoEq, false));
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
