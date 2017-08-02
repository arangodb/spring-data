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

package com.arangodb.springframework.core;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.ArangoDB;
import com.arangodb.internal.Host;

/**
 * @author Mark Vollmary
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/template-test-xml_extension-bean.xml" })
public class ArangoFactoryBeanTest {

	@Autowired
	private ApplicationContext context;

	@SuppressWarnings("unchecked")
	@Test
	public void arango() throws Exception {
		final ArangoDB.Builder arango = context.getBean("arango", ArangoDB.Builder.class);
		checkField(arango, "user", "testuser");
		checkField(arango, "password", "testpw");
		final List<Host> hosts = (List<Host>) getField(arango, "hosts");
		assertThat(hosts.size(), is(2));
		assertThat(hosts.get(1).getHost(), is("testhost"));
		assertThat(hosts.get(1).getPort(), is(1234));
	}

	@Test
	public void db() {
		final String db = context.getBean("database", String.class);
		assertThat(db, is("testdb"));
	}

	private Object getField(final ArangoDB.Builder arango, final String field)
			throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		final Field f = ArangoDB.Builder.class.getDeclaredField(field);
		f.setAccessible(true);
		return f.get(arango);
	}

	private void checkField(final ArangoDB.Builder arango, final String field, final Object expectedValue)
			throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		assertThat(getField(arango, field), is(expectedValue));
	}

}
