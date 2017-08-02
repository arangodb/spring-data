package com.arangodb.springframework.core.repository;

import com.arangodb.springframework.annotation.EnableArangoRepositories;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

import java.lang.annotation.Annotation;

/**
 * Created by F625633 on 07/07/2017.
 */
public class ArangoRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {
	@Override protected Class<? extends Annotation> getAnnotation() {
		return EnableArangoRepositories.class;
	}

	@Override protected RepositoryConfigurationExtension getExtension() {
		return new ArangoRepositoryConfigurationExtension();
	}
}
