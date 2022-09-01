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

package com.arangodb.springframework.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.repository.query.QueryTransactionBridge;

/**
 * To enable stream transactions for Arango Spring Data, create a
 * {@link org.springframework.context.annotation.Configuration} class annotated with
 * {@link org.springframework.transaction.annotation.EnableTransactionManagement} and
 * {@link org.springframework.context.annotation.Import} this one.
 */
public class ArangoTransactionManagementConfigurer implements TransactionManagementConfigurer {

    @Autowired
    private ArangoOperations operations;
    private final QueryTransactionBridge bridge = new QueryTransactionBridge();

    @Override
    @Bean
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return new ArangoTransactionManager(operations, bridge);
    }

    @Bean
    QueryTransactionBridge arangoQueryTransactionBridge() {
        return bridge;
    }
}
