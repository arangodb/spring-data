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

package com.arangodb.springframework.repository.query;

import org.springframework.core.NamedInheritableThreadLocal;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.function.Function;

/**
 * Bridge to postpone late transaction start to be able to inject collections from query side.
 *
 * @author Arne Burmeister
 */
public class QueryTransactionBridge {

    private static final ThreadLocal<Function<Collection<String>, String>> CURRENT_TRANSACTION = new NamedInheritableThreadLocal<Function<Collection<String>, String>>("ArangoTransactionBegin") {
        @Override
        protected Function<Collection<String>, String> initialValue() {
            return any -> null;
        }
    };

    /**
     * Prepare the bridge for accepting transaction begin.
     * @param begin a function accepting collection names and returning a stream transaction id
     *
     * @see com.arangodb.springframework.transaction.ArangoTransactionManager
     */
    public void setCurrentTransaction(Function<Collection<String>, String> begin) {
        CURRENT_TRANSACTION.set(begin);
    }

    /**
     * Reset the bridge ignoring transaction begin.
     *
     * @see com.arangodb.springframework.transaction.ArangoTransactionManager
     */
    public void clearCurrentTransaction() {
        CURRENT_TRANSACTION.remove();
    }

    /**
     * Applies the collection names to any current transaction.
     * @param collections additional collection names
     * @return the stream transaction id or {@code null} without transaction
     *
     * @see AbstractArangoQuery
     * @see com.arangodb.springframework.repository.SimpleArangoRepository
     */
    @Nullable
    public String getCurrentTransaction(Collection<String> collections) {
        return CURRENT_TRANSACTION.get().apply(collections);
    }
}
