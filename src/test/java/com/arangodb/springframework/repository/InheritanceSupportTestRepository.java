package com.arangodb.springframework.repository;

import java.util.Map;

import com.arangodb.springframework.annotation.Param;
import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.core.convert.InheritanceSupportTest;

public interface InheritanceSupportTestRepository extends ArangoRepository<InheritanceSupportTest.PersonSuperClass> {
	@Query("FOR p IN personSuperClass FILTER p._id == @id RETURN p")
	Map<String, Object> findOne(@Param("id") String id);
}
