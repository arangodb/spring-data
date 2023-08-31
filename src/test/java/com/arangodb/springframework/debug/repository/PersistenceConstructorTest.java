/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
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

package com.arangodb.springframework.debug.repository;


import com.arangodb.entity.DocumentEntity;
import com.arangodb.springframework.AbstractArangoTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Michele Rastelli
 */
public class PersistenceConstructorTest extends AbstractArangoTest {

    @Test
    public void saveAndRead() {
        PersistenceConstructorClass entity = new PersistenceConstructorClass("myClass");
        DocumentEntity inserted = template.insert(entity);
        PersistenceConstructorClass read = template.find(inserted.getId(), PersistenceConstructorClass.class).get();
        assertThat(read, is(entity));
    }

}
