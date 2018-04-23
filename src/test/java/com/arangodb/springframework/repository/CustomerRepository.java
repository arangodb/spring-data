package com.arangodb.springframework.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoPage;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.BaseDocument;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.annotation.BindVars;
import com.arangodb.springframework.annotation.Param;
import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.annotation.QueryOptions;
import com.arangodb.springframework.repository.query.derived.geo.Ring;
import com.arangodb.springframework.testdata.Customer;

/**
 * Created by F625633 on 07/07/2017.
 */
public interface CustomerRepository extends ArangoRepository<Customer> {

	@Query("FOR c IN customer FILTER c._id == @id RETURN c")
	Map<String, Object> findOneByIdAqlWithNamedParameter(@Param("id") String idString, AqlQueryOptions options);

	@Query("FOR c IN customer FILTER c.name == @1 AND c._id == @0 RETURN c")
	@QueryOptions(cache = true, ttl = 128)
	BaseDocument findOneByIdAndNameAql(String id, String name);

	@Query("FOR c IN customer FILTER c._id == @0 RETURN c")
	Optional<Customer> findOneByIdAqlPotentialNameClash(@Param("0") String id);

	@Query("FOR c IN customer FILTER c._id == @0 AND c.name == @0 RETURN c")
	ArangoCursor<Customer> findOneByIdAqlParamNameClash(String id, @Param("0") String name);

	@QueryOptions(maxPlans = 1000, ttl = 128)
	@Query("FOR c IN customer FILTER c._id == @id AND c.name == @name RETURN c")
	ArangoCursor<Customer> findOneByBindVarsAql(
		AqlQueryOptions options,
		@SuppressWarnings("rawtypes") @BindVars Map bindVars);

	@Query("FOR c IN customer FILTER c._id == @id AND c.name == @name RETURN c")
	Customer findOneByNameAndBindVarsAql(@Param("name") String name, @BindVars Map<String, Object> bindVars);

	@Query("FOR c IN customer FILTER c._id == @id AND c.name == @name RETURN c")
	Customer findOneByBindVarsAndClashingParametersAql(
		@BindVars Map<String, Object> bindVars,
		@Param("name") String name,
		AqlQueryOptions options,
		@Param("name") String name2);

	@Query("FOR c IN customer FILTER c.name == @name RETURN c")
	Customer findOneByNameWithDuplicateOptionsAql(
		@Param("name") String name,
		AqlQueryOptions options,
		AqlQueryOptions options2);

	@Query("FOR c IN customer FILTER c._id == @id AND c.name == @0 RETURN c")
	Customer findOneByIdAndNameWithBindVarsAql(String name, @BindVars Map<String, Object> bindVars);

	@Query("FOR c IN @@0 FILTER \"@1\" != '@2' AND c._id == @1 RETURN c")
	Customer findOneByIdInCollectionAql(String collection, String id, String id2);

	@Query("FOR c IN @@collection FILTER \"\\\"@1\\\"\" != '\"@2\"' AND c._id == @1 RETURN c")
	Customer findOneByIdInNamedCollectionAql(@Param("@collection") String collection, String id, String id2);

	@Query("FOR c IN @@collection FILTER \"'@1'\" != '\\'@2\\'' AND c._id == @1 RETURN c")
	Customer findOneByIdInIncorrectNamedCollectionAql(@Param("collection") String collection, String id, String id2);

	@Query("FOR c IN @collection FILTER c._id == @1 RETURN c")
	Customer findOneByIdInNamedCollectionAqlRejected(@Param("collection") String collection, String id);

	@Query("FOR c in customer FILTER c.surname == @0 RETURN c")
	List<Customer> findManyBySurname(String surname);

	Set<Customer> findDistinctByNameAfter(String name);

	List<Customer> findByNameNotIgnoreCaseAndAgeLessThanIgnoreCaseOrderByNameDesc(String name, int age);

	Iterable<Customer> findTop3ByAgeInAndStringArrayIgnoreCaseOrNameNotInAndIntegerListIgnoreCaseOrderByAgeAscNameDescAllIgnoreCase(
		int[] ages,
		String[] stringArray,
		String[] names,
		List<Integer> integerList);

	Customer[] findDistinctByAgeGreaterThanEqualOrStringArrayAndNameBeforeOrIntegerListOrderByNameAscAgeAscAllIgnoreCase(
		int age,
		String[] stringArray,
		String name,
		List<Integer> integerList);

	Collection<Customer> findTop2DistinctByStringArrayContainingIgnoreCaseOrIntegerListNotNullIgnoreCaseOrderByNameAsc(
		String string);

	Collection<Customer> findByStringArrayNotContainingIgnoreCase(String string);

	int countByAgeGreaterThanOrStringArrayNullAndIntegerList(int age, List<Integer> integerList);

	Integer countDistinctByAliveTrueOrNameLikeOrAgeLessThanEqual(String pattern, int age);

	Customer findByNameStartsWithAndSurnameEndsWithAndAgeBetween(
		String prefix,
		String suffix,
		int lowerBound,
		int upperBound);

	void removeByNameNotLikeAndSurnameRegexOrAliveFalse(String pattern, String regex);

	// GEOSPATIAL

	Customer[] findByLocationNear(Point location);

	List<Customer> findByLocationWithinAndName(Point location, Range<Double> distanceRange, String name);

	Iterable<Customer> findByLocationWithinOrNameAndLocationNear(Circle circle, String name, Point location2);

	List<Customer> findByLocationWithin(Box box);

	Collection<Customer> findByLocationWithinAndLocationWithinOrName(
		Point location,
		int distance,
		Ring<?> ring,
		String name);

	List<Customer> findByLocationWithin(Polygon polygon);

	List<Customer> findByNameOrLocationWithinOrNameAndSurnameOrNameAndLocationNearAndSurnameAndLocationWithin(
		String name1,
		Point location1,
		double distance,
		String name2,
		String surname1,
		String name3,
		Point location2,
		String surname2,
		Point location3,
		Range<Double> distanceRange);

	// EXISTS

	boolean existsByName(String name);

	Customer[] findByNestedCustomerAliveExistsAndStringListAllIgnoreCase(List<String> stringList);

	// SORT

	Customer[] findByNameOrderBySurnameAsc(Sort sort, String name);

	// PAGEABLE

	Page<Customer> readByNameAndSurname(Pageable pageable, String name, AqlQueryOptions options, String surname);

	// GEO_RESULT, GEO_RESULTS, GEO_PAGE

	GeoResult<Customer> queryByLocationWithin(Point location, double distance);

	GeoResults<Customer> findByLocationWithin(Point location, Range<Double> distanceRange);

	GeoPage<Customer> findByLocationNear(Point location, Pageable pageable);

	GeoResults<Customer> findByNameOrSurnameAndLocationWithinOrLocationWithin(
		String name,
		String surname,
		Point location1,
		Distance distance,
		Point location2,
		Range<Distance> distanceRange);

	// NESTED PROPERTIES

	List<Customer> findByNestedCustomerName(String name);

	// REFERENCES

	List<Customer> findByNestedCustomersNestedCustomerShoppingCartProductsLocationWithin(
		Point location,
		double distance);

	List<Customer> findByNestedCustomersNestedCustomerShoppingCartProductsNameLike(String name);

	// Graph traversal

	List<Customer> getByOwnsName(String name);

	List<Customer> getByOwnsContainsName(String name);

	@Query("RETURN COUNT(@@collection)")
	long queryCount(@Param("@collection") Class<Customer> collection);

	@Query("RETURN DATE_ISO8601(1474988621)")
	Instant queryDate();
}
