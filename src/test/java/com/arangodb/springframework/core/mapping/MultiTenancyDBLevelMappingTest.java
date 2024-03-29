/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
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

package com.arangodb.springframework.core.mapping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.arangodb.springframework.ArangoMultiTenancyTestConfiguration;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.component.TenantProvider;
import com.arangodb.springframework.core.ArangoOperations;

/**
 * @author Mark Vollmary
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ArangoMultiTenancyTestConfiguration.class })
public class MultiTenancyDBLevelMappingTest {

	private static final String TENANT00 = "tenant00";
	private static final String TENANT01 = "tenant01";

	@Autowired
	protected ArangoOperations template;

	@Document("#{tenantProvider.getId()}_collection")
	static class MultiTenancyTestEntity {

	}

	@Autowired
	TenantProvider tenantProvider;

	@Test
	public void dbAndCollectionLevel() {
		{
			tenantProvider.setId(TENANT00);
			template.insert(new MultiTenancyTestEntity());
			assertThat(template.driver().db(ArangoMultiTenancyTestConfiguration.DB + TENANT00)
					.collection(TENANT00 + "_collection").exists(),
				is(true));
		}
		{
			tenantProvider.setId(TENANT01);
			template.insert(new MultiTenancyTestEntity());
			assertThat(template.driver().db(ArangoMultiTenancyTestConfiguration.DB + TENANT01)
					.collection(TENANT01 + "_collection").exists(),
				is(true));
		}
		assertThat(template.driver().db(ArangoMultiTenancyTestConfiguration.DB + TENANT00)
				.collection(TENANT00 + "_collection").count().getCount(),
			is(1L));
		assertThat(template.driver().db(ArangoMultiTenancyTestConfiguration.DB + TENANT01)
				.collection(TENANT01 + "_collection").count().getCount(),
			is(1L));
	}

	@BeforeEach
	@AfterEach
	public void cleanup() {
		tenantProvider.setId(TENANT00);
		template.dropDatabase();
		tenantProvider.setId(TENANT01);
		template.dropDatabase();
	}

}
