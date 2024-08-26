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

package com.arangodb.springframework.core.template;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.*;
import com.arangodb.model.*;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.repository.IdTestEntity;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractTestEntityTemplateTest<T extends IdTestEntity<?>> extends AbstractArangoTest {

    protected abstract T createEntity();

    private final Class<T> clazz;

    public AbstractTestEntityTemplateTest(Class<T> clazz) {
        super(clazz);
        this.clazz = clazz;
    }

    @Test
    public void insertDocument() {
        DocumentEntity res = template.insert(createEntity());
        assertThat(res).isNotNull();
        assertThat(res.getId()).isNotNull();
    }

    @Test
    public void repsertDocument() {
        T doc = createEntity();
        T repsert1 = template.repsert(doc);
        T repsert2 = template.repsert(doc);
        assertThat(repsert1).isEqualTo(doc);
        assertThat(repsert2).isEqualTo(doc);
    }

    @Test
    public void getDocument() {
        T doc = createEntity();
        DocumentEntity res = template.insert(doc);
        T customer = template.find(res.getId(), clazz).get();
        assertThat(customer).isEqualTo(doc);
    }

    @Test
    public void replaceDocument() {
        T doc = createEntity();
        T doc2 = createEntity();
        DocumentEntity res = template.insert(doc);
        DocumentEntity replaceDocument = template.replace(res.getId(), doc2);
        assertThat(replaceDocument).isNotNull();
        T read = template.find(res.getId(), clazz).get();
        assertThat(read).isEqualTo(doc2);
    }

	@Test
	public void updateDocument() {
        T doc = createEntity();
        T doc2 = createEntity();
        DocumentEntity res = template.insert(doc);
		template.update(res.getId(), doc2);
		T read = template.find(res.getId(), clazz).get();
		assertThat(read).isEqualTo(doc2);
	}

	@Test
	public void deleteDocument() {
        T doc = createEntity();
		DocumentEntity res = template.insert(doc);
		template.delete(res.getId(), clazz);
		Optional<T> read = template.find(res.getId(), clazz);
		assertThat(read).isNotPresent();
	}

	@Test
	public void query() {
        T doc = createEntity();
		template.insert(doc);
		Map<String, Object> bindVars = new HashMap<>();
		bindVars.put("@coll", template.collection(clazz).name());
		bindVars.put("value", doc.getValue());
		ArangoCursor<T> cursor = template.query("FOR c IN @@coll FILTER c.value == @value RETURN c",
			bindVars, new AqlQueryOptions(), clazz);
		List<T> customers = cursor.asListRemaining();
		assertThat(customers).hasSize(1);
		assertThat(customers.get(0)).isEqualTo(doc);
	}

}
