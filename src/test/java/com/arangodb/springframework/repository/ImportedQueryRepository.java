package com.arangodb.springframework.repository;

import java.util.List;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.testdata.Customer;

@NoRepositoryBean
public interface ImportedQueryRepository {

	@Query("FOR c in #collection FILTER c.surname == @param RETURN c")
	List<Customer> importedQuery(@Param("param") String param);

}
