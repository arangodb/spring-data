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

import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.CollectionOperations;
import org.springframework.dao.DataAccessException;

/**
 * Internal interface to handle collection operations.
 * Typically implemented by same class as {@link com.arangodb.springframework.core.ArangoOperations}.
 */
public interface CollectionCallback {

    /**
     * @see com.arangodb.springframework.core.ArangoOperations#collection(Class)
     */
    CollectionOperations collection(Class<?> type) throws DataAccessException;

    /**
     * @see com.arangodb.springframework.core.ArangoOperations#collection(String)
     */
    CollectionOperations collection(String name) throws DataAccessException;


    static CollectionCallback fromOperations(ArangoOperations operations) {
        if (operations instanceof CollectionCallback) {
            return (CollectionCallback) operations;
        }
        return new CollectionCallback() {
            @Override
            public CollectionOperations collection(Class<?> type) {
                return operations.collection(type);
            }

            @Override
            public CollectionOperations collection(String name) {
                return operations.collection(name);
            }
        };
    }

}
