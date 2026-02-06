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

package com.arangodb.springframework;

import com.arangodb.ArangoDatabase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import com.arangodb.springframework.core.ArangoOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Vollmary
 */
@SpringJUnitConfig(ArangoTestConfiguration.class)
@DirtiesContext // Avoid parallel execution because the database is deleted after test and the name is the same for all tests
public abstract class AbstractArangoTest {

	private static volatile ArangoOperations staticTemplate;

	@Autowired
	protected ArangoOperations template;
	protected ArangoDatabase db;
	protected final Class<?>[] collections;

	protected AbstractArangoTest(final Class<?>... collections) {
		super();
		this.collections = collections;
	}

	@BeforeEach
	public void before() {
		for (final Class<?> collection : collections) {
			template.collection(collection).truncate();
		}
		AbstractArangoTest.staticTemplate = template;
		db = template.driver().db(ArangoTestConfiguration.DB);
	}

	@AfterAll
	public static void afterClass() {
		staticTemplate.dropDatabase();
	}

    public boolean isAtLeastVersion(final int major, final int minor) {
        return isAtLeastVersion(major, minor, 0);
    }

    public boolean isAtLeastVersion(final int major, final int minor, final int patch) {
        return TestUtils.isAtLeastVersion(template.getVersion().getVersion(), major, minor, patch);
    }

    public boolean isLessThanVersion(final int major, final int minor) {
        return isLessThanVersion(major, minor, 0);
    }

    public boolean isLessThanVersion(final int major, final int minor, final int patch) {
        return TestUtils.isLessThanVersion(template.getVersion().getVersion(), major, minor, patch);
    }

}
