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

import java.util.Optional;

import com.arangodb.springframework.repository.StringIdTestEntity;
import com.arangodb.springframework.repository.StringIdTestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.arangodb.springframework.ArangoMultiTenancyRepositoryTestConfiguration;
import com.arangodb.springframework.component.TenantProvider;
import com.arangodb.springframework.core.ArangoOperations;

/**
 * @author Paulo Ferreira
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ArangoMultiTenancyRepositoryTestConfiguration.class})
public class MultiTenancyDBLevelRepositoryTest {

    private static final String TENANT00 = "tenant00";

    @Autowired
    protected ArangoOperations template;

    @Autowired
    TenantProvider tenantProvider;

    @Autowired
    StringIdTestRepository idTestRepository;

    /*
     * For some reason, findAll already creates the collection by itself
     */

    @Test
    public void dbFindAll() {
        tenantProvider.setId(TENANT00);
        assertThat(idTestRepository.findAll().iterator().hasNext(), is(false));
    }

    @Test
    public void dbFindOne() {
        tenantProvider.setId(TENANT00);
        StringIdTestEntity entity = new StringIdTestEntity();
        entity.setId("MyId");
        Example<StringIdTestEntity> example = Example.of(entity);
        Optional<StringIdTestEntity> result = idTestRepository.findOne(example);
        assertThat(result.isPresent(), is(false));
    }

    @BeforeEach
    @AfterEach
    public void cleanup() {
        tenantProvider.setId(TENANT00);
        template.dropDatabase();
    }

}