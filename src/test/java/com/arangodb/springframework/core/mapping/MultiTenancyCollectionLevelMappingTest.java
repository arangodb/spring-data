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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.component.TenantProvider;

/**
 * @author Mark Vollmary
 *
 */
public class MultiTenancyCollectionLevelMappingTest extends AbstractArangoTest {

	@Document("#{tenantProvider.getId()}_collection")
	static class MultiTenancyTestEntity {

	}

	@Autowired
	TenantProvider tenantProvider;

	@Test
	public void collectionLevel() {
		{
			tenantProvider.setId("tenant00");
			template.insert(new MultiTenancyTestEntity());
			assertThat(db.collection("tenant00_collection").exists(),
				is(true));
		}
		{
			tenantProvider.setId("tenant01");
			template.insert(new MultiTenancyTestEntity());
			assertThat(db.collection("tenant01_collection").exists(),
				is(true));
		}
		assertThat(
			db.collection("tenant00_collection").count().getCount(),
			is(1L));
		assertThat(
			db.collection("tenant01_collection").count().getCount(),
			is(1L));
	}

}
