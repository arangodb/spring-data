package com.arangodb.springframework.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.query.Param;

import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.testdata.HumanBeing;

/**
 * Provides in-bound, & out-bound graph traversal queries.
 * 
 * @author Reşat SABIQ
 */
public interface HumanBeingRepository extends ArangoRepository<HumanBeing> {
	Optional<HumanBeing> findByNameAndSurname(String name, String surname);
	@Query("FOR v IN 1..2 INBOUND @id @@edgeCol SORT v.age DESC RETURN DISTINCT v")
	List<HumanBeing> getAllChildrenAndGrandchildren(@Param("id") String id, @Param("@edgeCol") Class<?> edgeCollection);
	@Query("FOR v IN 1..@max INBOUND @id @@edgeCol SORT v.age DESC RETURN DISTINCT v")
	List<HumanBeing> getAllChildrenMultilevel(@Param("id") String id, @Param("max") byte max, @Param("@edgeCol") Class<?> edgeCollection);
	@Query("FOR vertex IN 1..@max OUTBOUND @id @@edgeCol SORT vertex.age DESC RETURN DISTINCT vertex")
	List<HumanBeing> getAllParentsMultilevel(@Param("id") String id, @Param("max") byte max, @Param("@edgeCol") Class<?> edgeCollection);
}
