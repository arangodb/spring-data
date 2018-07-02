package com.arangodb.springframework.repository;

import java.util.Map;

import org.springframework.data.repository.query.Param;

import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.core.mapping.InheritanceMappingFocusingOnClassesNotRequiringPersistenceOfTypeDataTest;

/**
 * 
 * @author Reşat SABIQ
 */
public interface InheritanceMappingFocusingOnClassesNotRequiringPersistenceOfTypeDataRepository extends ArangoRepository<InheritanceMappingFocusingOnClassesNotRequiringPersistenceOfTypeDataTest.PersonSuperClass> {
	@Query("FOR p IN person FILTER p._id == @id RETURN p")
	Map<String, Object> findOne(@Param("id") String id);
}
