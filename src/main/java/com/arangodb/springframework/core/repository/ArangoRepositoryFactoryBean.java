package com.arangodb.springframework.core.repository;

import com.arangodb.springframework.core.ArangoOperations;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.Assert;

/**
 * Created by F625633 on 07/07/2017.
 */
public class ArangoRepositoryFactoryBean<T extends Repository<S, String>, S> extends RepositoryFactoryBeanSupport<T, S, String> {
	private ArangoOperations arangoOperations;

	
	public ArangoRepositoryFactoryBean(Class<? extends T> repositoryInterface, ArangoOperations arangoOperations) {
		super(repositoryInterface);
		this.arangoOperations = arangoOperations;
	}

	@Override protected RepositoryFactorySupport createRepositoryFactory() {
		Assert.notNull(arangoOperations, "arangoOperations not configured");
		return new ArangoRepositoryFactory(arangoOperations);
	}
}
