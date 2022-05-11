/*
 * DISCLAIMER
 *
 * Copyright 2017 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.springframework.repository;

import com.arangodb.springframework.config.ArangoConfiguration;
import com.arangodb.springframework.core.template.ArangoTemplate;
import com.arangodb.springframework.repository.query.QueryTransactionBridge;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.Assert;

/**
 * Created by F625633 on 07/07/2017.
 */
public class ArangoRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
		extends RepositoryFactoryBeanSupport<T, S, ID>  implements ApplicationContextAware {

	private ArangoTemplate arangoTemplate;
	private ApplicationContext applicationContext;
	private QueryTransactionBridge transactionBridge;
	private ArangoConfiguration arangoConfiguration;

	@Autowired
	public ArangoRepositoryFactoryBean(final Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	@Autowired
	public void setArangoTemplate(final ArangoTemplate arangoTemplate) {
		this.arangoTemplate = arangoTemplate;
	}

	@Autowired
	public void setTransactionBridge(final QueryTransactionBridge transactionBridge) {
		this.transactionBridge = transactionBridge;
	}

	@Autowired
	public void setArangoConfiguration(final ArangoConfiguration arangoConfiguration) {
		this.arangoConfiguration = arangoConfiguration;
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {
		Assert.notNull(arangoTemplate, "arangoOperations not configured");
		return new ArangoRepositoryFactory(arangoTemplate, applicationContext, transactionBridge, arangoConfiguration);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
