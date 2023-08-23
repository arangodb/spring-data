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

import com.arangodb.entity.StreamTransactionEntity;
import com.arangodb.entity.StreamTransactionStatus;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Synchronisation resource (has to be mutable).
 *
 * @see TransactionSynchronizationManager#bindResource(Object, Object)
 * @see ArangoTransactionObject
 * @author Arne Burmeister
 */
class ArangoTransactionHolder {

    private final Set<String> collectionNames = new HashSet<>();
    private StreamTransactionEntity transaction = null;
    private boolean rollbackOnly = false;

    @Nullable
    String getStreamTransactionId() {
        return transaction == null ? null : transaction.getId();
    }

    void setStreamTransaction(StreamTransactionEntity transaction) {
        this.transaction = transaction;
    }

    Set<String> getCollectionNames() {
        return collectionNames;
    }

    void addCollectionNames(Collection<String> collectionNames) {
        if (transaction != null) {
            throw new IllegalStateException("Collections must not be added after stream transaction begun");
        }
        this.collectionNames.addAll(collectionNames);
    }

    boolean isRollbackOnly() {
        return rollbackOnly || isStatus(StreamTransactionStatus.aborted);
    }

    void setRollbackOnly() {
        rollbackOnly = true;
    }

    public boolean isStatus(StreamTransactionStatus status) {
        return transaction != null && transaction.getStatus() == status;
    }
}
