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

import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.StreamTransactionStatus;
import com.arangodb.model.StreamTransactionOptions;
import com.arangodb.springframework.core.template.CollectionCallback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.support.SmartTransactionObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.transaction.TransactionDefinition.TIMEOUT_DEFAULT;

/**
 * Transaction object created by
 * {@link ArangoTransactionManager#doGetTransaction()}.
 */
class ArangoTransactionObject implements SmartTransactionObject {

    private static final Log logger = LogFactory.getLog(ArangoTransactionObject.class);

    private final ArangoDatabase database;
    private final CollectionCallback collectionCallback;
    private final ArangoTransactionHolder holder;
    private int timeout;

    ArangoTransactionObject(ArangoDatabase database, CollectionCallback collectionCallback, int defaultTimeout, @Nullable ArangoTransactionHolder holder) {
        this.database = database;
        this.collectionCallback = collectionCallback;
        this.holder = holder == null ? new ArangoTransactionHolder() : holder;
        this.timeout = defaultTimeout;
    }

    ArangoTransactionHolder getHolder() {
        return holder;
    }

    void configure(TransactionDefinition definition) {
        if (definition.getTimeout() != TIMEOUT_DEFAULT) {
            this.timeout = definition.getTimeout();
        }
        if (definition instanceof TransactionAttribute) {
            addCollections(((TransactionAttribute) definition).getLabels());
        }
    }

    ArangoTransactionHolder getOrBegin(Collection<String> collections) throws ArangoDBException {
        addCollections(collections);
        if (holder.getStreamTransactionId() == null) {
            holder.getCollectionNames().forEach(collectionCallback::collection);
            StreamTransactionOptions options = new StreamTransactionOptions()
                    .allowImplicit(true)
                    .writeCollections(holder.getCollectionNames().toArray(new String[0]))
                    .lockTimeout(Math.max(timeout, 0));
            holder.setStreamTransaction(database.beginStreamTransaction(options));
            if (logger.isDebugEnabled()) {
                logger.debug("Began stream transaction " + holder.getStreamTransactionId() + " writing collections " + holder.getCollectionNames());
            }
        }
        return getHolder();
    }

    void commit() throws ArangoDBException {
        if (holder.isStatus(StreamTransactionStatus.running)) {
            holder.setStreamTransaction(database.commitStreamTransaction(holder.getStreamTransactionId()));
        }
    }

    void rollback() throws ArangoDBException {
        holder.setRollbackOnly();
        if (holder.isStatus(StreamTransactionStatus.running)) {
            holder.setStreamTransaction(database.abortStreamTransaction(holder.getStreamTransactionId()));
        }
    }

    @Override
    public boolean isRollbackOnly() {
        return holder.isRollbackOnly();
    }

    @Override
    public void flush() {
    }

    @Override
    public String toString() {
        return holder.getStreamTransactionId() == null ? "(not begun)" : holder.getStreamTransactionId();
    }

    private void addCollections(Collection<String> collections) {
        if (holder.getStreamTransactionId() == null) {
            holder.addCollectionNames(collections);
        } else if (logger.isDebugEnabled() && !holder.getCollectionNames().containsAll(collections)) {
            Set<String> additional = new HashSet<>(collections);
            additional.removeAll(holder.getCollectionNames());
            logger.debug("Stream transaction already started on collections " + holder.getCollectionNames() + ", assuming additional collections are read only: " + additional);
        }
    }
}
