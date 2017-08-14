package com.arangodb.springframework.core.repository;

import com.arangodb.entity.BaseDocument;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.annotation.BindVars;
import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.core.repository.query.derived.Disjunction;
import com.arangodb.springframework.core.repository.query.derived.geo.Range;
import com.arangodb.springframework.testdata.Customer;
import com.arangodb.springframework.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;

import java.util.*;

/**
 * Created by F625633 on 07/07/2017.
 */
public interface CustomerRepository extends ArangoRepository<Customer>{

	@Query("FOR c IN customer FILTER c._id == @id RETURN c")
	Map<String, Object> findOneByIdAqlWithNamedParameter(@Param("id") String idString, AqlQueryOptions options);

	@Query("FOR c IN customer FILTER c.name == @1 AND c._id == @0 RETURN c")
	BaseDocument findOneByIdAndNameAql(String id, String name);

	@Query("FOR c IN customer FILTER c._id == @0 RETURN c")
	Optional<Customer> findOneByIdAqlPotentialNameClash(@Param("0") String id);

	@Query("FOR c IN customer FILTER c._id == @0 AND c.name == @0 RETURN c")
	Customer findOneByIdAqlParamNameClash(String id, @Param("0") String name);

	@Query("FOR c IN customer FILTER c._id == @id AND c.name == @name RETURN c")
	Customer findOneByBindVarsAql(AqlQueryOptions options, @BindVars Map bindVars);

	@Query("FOR c IN customer FILTER c._id == @id AND c.name == @name RETURN c")
	Customer findOneByNameAndBindVarsAql(@Param("name") String name, @BindVars Map bindVars);

	@Query("FOR c IN customer FILTER c._id == @id AND c.name == @name RETURN c")
	Customer findOneByBindVarsAndClashingParametersAql(
			@BindVars Map bindVars, @Param("name") String name, AqlQueryOptions options, @Param("name") String name2
	);

	@Query("FOR c IN customer FILTER c.name == @name RETURN c")
	Customer findOneByNameWithDuplicateOptionsAql(
			@Param("name") String name, AqlQueryOptions options, AqlQueryOptions options2
	);

	@Query("FOR c IN customer FILTER c._id == @id AND c.name == @0 RETURN c")
	Customer findOneByIdAndNameWithBindVarsAql(String name, @BindVars Map bindVars);

	@Query("FOR c IN @@0 FILTER \"@1\" != '@2' AND c._id == @1 RETURN c")
	Customer findOneByIdInCollectionAql(String collection, String id, String id2);

	@Query("FOR c IN @@collection FILTER \"\\\"@1\\\"\" != '\"@2\"' AND c._id == @1 RETURN c")
	Customer findOneByIdInNamedCollectionAql(@Param("@collection") String collection, String id, String id2);

	@Query("FOR c IN @@collection FILTER \"'@1'\" != '\\'@2\\'' AND c._id == @1 RETURN c")
	Customer findOneByIdInIncorrectNamedCollectionAql(@Param("collection") String collection, String id, String id2);

	@Query("FOR c IN @collection FILTER c._id == @1 RETURN c")
	Customer findOneByIdInNamedCollectionAqlRejected(@Param("collection") String collection, String id);

	Set<Customer> findDistinctByNameAfter(String name);

	List<Customer> findByNameNotIgnoreCaseAndAgeLessThanIgnoreCaseOrderByNameDesc(String name, int age);

	Iterable<Customer> findTop3ByAgeInAndStringArrayIgnoreCaseOrNameNotInAndIntegerListIgnoreCaseOrderByAgeAscNameDescAllIgnoreCase(int[] ages, String[] stringArray, String[] names, List<Integer> integerList);

	Customer[] findDistinctByAgeGreaterThanEqualOrStringArrayAndNameBeforeOrIntegerListOrderByNameAscAgeAscAllIgnoreCase(int age, String[] stringArray, String name, List<Integer> integerList);

	Collection<Customer> findTop2DistinctByStringArrayContainingIgnoreCaseOrIntegerListNotNullIgnoreCaseOrderByNameAsc(String string);

	int countByAgeGreaterThanOrStringArrayNullAndIntegerList(int age, List<Integer> integerList);

	Integer countDistinctByAliveTrueOrNameLikeOrAgeLessThanEqual(String pattern, int age);

	Customer findByNameStartsWithAndSurnameEndsWithAndAgeBetween(String prefix, String suffix, int lowerBound, int upperBound);

	void removeByNameNotLikeAndSurnameRegexOrAliveFalse(String pattern, String regex);

	// GEOSPATIAL

	Customer[] findByLocationNear(Point location);

	List<Customer> findByLocationWithinAndName(Point location, Range<Double> distanceRange, String name);

	Iterable<Customer> findByLocationWithinOrNameAndLocationNear(Point location, Distance distance, String name, Point location2);

	Collection<Customer> findByLocationWithinAndLocationWithinOrName(Point location, int distance, Point location2, Range distanceRange, String name);

	// ArrayList not supported, use List instead
	List<Customer> findByNameOrLocationWithinOrNameAndSurnameOrNameAndLocationNearAndSurnameAndLocationWithin(
			String name1, Point location1, double distance, String name2, String surname1, String name3, Point location2, String surname2, Point location3, Range<Double> distanceRange);

	// EXISTS

	Customer[] findByAliveExistsAndStringListAllIgnoreCase(List<String> stringList);

	// SORT

	Customer[] findByNameOrderBySurnameAsc(Sort sort, String name);

	// PAGEABLE

	Page<Customer> readByNameAndSurname(Pageable pageable, String name, AqlQueryOptions options, String surname);

	// GEO_RESULT, GEO_RESULTS, GEO_PAGE

	GeoResult<Customer> queryByLocationWithin(Point location, double distance);

	GeoResults<Customer> findByLocationWithin(Point location, Range<Double> distanceRange);

	GeoPage<Customer> findByLocationNear(Point location, Pageable pageable);

	GeoResults<Customer> findByNameOrSurnameAndLocationWithinOrLocationWithin(String name, String surname, Point location1, double distance, Point location2, Range distanceRange);
}
