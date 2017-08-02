package com.arangodb.springframework.core.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Created by F625633 on 06/07/2017.
 */
@NoRepositoryBean
public interface ArangoRepository<T> extends PagingAndSortingRepository<T, String> {
}
