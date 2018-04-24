package com.arangodb.springframework.repository;

import java.util.Map;

import com.arangodb.springframework.annotation.Param;
import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.core.convert.InheritanceSupportTest;

/**
 * 
 * @author Reşat SABIQ
 */
public interface InheritanceSupportTestRepository extends ArangoRepository<InheritanceSupportTest.PersonSuperClass> {
	@Query("FOR p IN person FILTER p._id == @id RETURN p")
	Map<String, Object> findOne(@Param("id") String id);
}
