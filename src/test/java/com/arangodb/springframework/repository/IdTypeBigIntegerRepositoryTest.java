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

package com.arangodb.springframework.repository;

import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;

public class IdTypeBigIntegerRepositoryTest extends AbstractTestEntityRepositoryTest<BigIntegerIdTestEntity, BigInteger> {
    @Autowired
    private BigIntegerIdTestRepository repository;

    public IdTypeBigIntegerRepositoryTest() {
        super(BigIntegerIdTestEntity.class);
    }

    @Override
    protected IdTestRepository<BigIntegerIdTestEntity, BigInteger> repository() {
        return repository;
    }

    @Override
    protected BigIntegerIdTestEntity createEntity() {
        return new BigIntegerIdTestEntity();
    }
}
