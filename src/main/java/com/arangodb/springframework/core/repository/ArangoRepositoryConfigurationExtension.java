package com.arangodb.springframework.core.repository;

import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;

/**
 * Created by F625633 on 07/07/2017.
 */
public class ArangoRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {
	@Override protected String getModulePrefix() {
		return null;
	}

	@Override public String getRepositoryFactoryClassName() {
		return ArangoRepositoryFactoryBean.class.getName();
	}
}
