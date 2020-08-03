package com.arangodb.springframework.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import org.springframework.data.repository.query.Param;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.BaseDocument;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.annotation.BindVars;
import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.annotation.QueryOptions;
import com.arangodb.springframework.repository.query.derived.geo.Ring;
import com.arangodb.springframework.testdata.Customer;
import com.arangodb.springframework.testdata.CustomerNameProjection;

/**
 * 
 * @author Audrius Malele
 * @author Mark McCormick
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public interface CustomerRepository extends ArangoRepository<Customer, String>, ImportedQueryRepository{

	@Query("FOR c IN customer FILTER c._key == @id RETURN c")
	Map<String, Object> findOneByIdAqlWithNamedParameter(@Param("id") String idString, AqlQueryOptions options);

	@Query("FOR c IN #collection FILTER c.name == @1 AND c._key == @0 RETURN c")
	@QueryOptions(cache = true, ttl = 128)
	BaseDocument findOneByIdAndNameAql(String id, String name);

	@QueryOptions(maxPlans = 1000, ttl = 128)
	@Query("FOR c IN #collection FILTER c._key == @id AND c.name == @name RETURN c")
	ArangoCursor<Customer> findOneByBindVarsAql(AqlQueryOptions options, @BindVars Map<String, Object> bindVars);

	@Query("FOR c IN #collection FILTER c._key == @id AND c.name == @name RETURN c")
	Customer findOneByNameAndBindVarsAql(@Param("name") String name, @BindVars Map<String, Object> bindVars);

	@Query("FOR c IN #collection FILTER c._key == @id AND c.name == @0 RETURN c")
	Customer findOneByIdAndNameWithBindVarsAql(String name, @BindVars Map<String, Object> bindVars);

	@Query("FOR c IN @@0 FILTER \"@1\" != '@2' AND c._id == @1 RETURN c")
	Customer findOneByIdInCollectionAqlWithUnusedParam(String collection, String id, String id2);

	@Query("FOR c IN @@collection FILTER \"\\\"@id\\\"\" != '\"@id2\"' AND c._id == @id RETURN c")
	Customer findOneByIdInNamedCollectionAqlWithUnusedParam(
		@Param("@collection") String collection,
		@Param("id") String id,
		@Param("id2") String id2);

	@Query("FOR c IN @@collection FILTER \"'@id'\" != '\\'@id2\\'' AND c._id == @id RETURN c")
	Customer findOneByIdInIncorrectNamedCollectionAql(
		@Param("collection") String collection,
		@Param("id") String id,
		@Param("id2") String id2);

	@Query("FOR c IN @collection FILTER c._id == @id RETURN c")
	Customer findOneByIdInNamedCollectionAqlRejected(@Param("collection") String collection, @Param("id") String id);

	@Query("FOR c in #collection FILTER c.surname == @0 RETURN c")
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

	Collection<Customer> findByNameContaining(String string);

	Collection<Customer> findByNameContainingIgnoreCase(String string);

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

	@Query("FOR c IN #collection FILTER c.name == @1 #sort RETURN c")
	List<Customer> findByNameWithSort(Sort sort, String name);

	// PAGEABLE

	Page<Customer> readByNameAndSurname(Pageable pageable, String name, AqlQueryOptions options, String surname);

	@Query("FOR c IN #collection FILTER c.name == @1 AND c.surname == @2 #pageable RETURN c")
	Page<Customer> findByNameAndSurnameWithPageable(Pageable pageable, String name, String surname);

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

	List<Customer> getByOwnsNameAndOwns2Name(String name, String name2);

	// Count query

	@Query("RETURN COUNT(@@collection)")
	long queryCount(@Param("@collection") Class<Customer> collection);

	// Date query

	@Query("RETURN DATE_ISO8601(1474988621)")
	Instant queryDate();

	// Named query

	Customer findOneByIdNamedQuery(@Param("id") String id);

	// Static projection

	@Query("FOR c IN #collection FILTER c._key == @id RETURN c")
	CustomerNameProjection findOneByIdWithStaticProjection(@Param("id") String id);

	@Query("FOR c IN #collection FILTER c.age >= 18 RETURN c")
	List<CustomerNameProjection> findManyLegalAgeWithStaticProjection();

	// Dynamic projection

	@Query("FOR c IN #collection FILTER c._key == @id RETURN c")
	<T> T findOneByIdWithDynamicProjection(@Param("id") String id, Class<T> projection);

	@Query("FOR c IN #collection FILTER c.age >= 18 RETURN c")
	<T> List<T> findManyLegalAgeWithDynamicProjection(Class<T> projection);

}
