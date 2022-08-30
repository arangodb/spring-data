package com.arangodb.springframework.transaction;

import com.arangodb.ArangoDatabase;
import com.arangodb.DbName;
import com.arangodb.entity.StreamTransactionEntity;
import com.arangodb.entity.StreamTransactionStatus;
import com.arangodb.model.StreamTransactionOptions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.support.SmartTransactionObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class ArangoTransactionObject implements SmartTransactionObject {

    private static final Log logger = LogFactory.getLog(ArangoTransactionObject.class);

    private final ArangoDatabase database;
    private final Set<String> writeCollections = new HashSet<>();
    private int timeout;
    private StreamTransactionEntity streamTransaction;

    ArangoTransactionObject(ArangoDatabase database, int defaultTimeout, @Nullable ArangoTransactionResource resource) {
        this.database = database;
        this.timeout = defaultTimeout;
        if (resource != null) {
            writeCollections.addAll(resource.getCollectionNames());
            if (resource.getStreamTransactionId() != null) {
                streamTransaction = database.getStreamTransaction(resource.getStreamTransactionId());
            }
        }
    }

    ArangoTransactionResource createResource() {
        return new ArangoTransactionResource(streamTransaction == null ? null : streamTransaction.getId(), writeCollections);
    }

    boolean exists() {
        return streamTransaction != null;
    }

    void configure(TransactionDefinition definition) {
        if (definition.getTimeout() != -1) {
            this.timeout = definition.getTimeout();
        }
        if (definition instanceof TransactionAttribute) {
            addCollections(((TransactionAttribute) definition).getLabels());
        }
    }

    ArangoTransactionResource getOrBegin(Collection<String> collections) {
        addCollections(collections);
        if (streamTransaction != null) {
            return createResource();
        }
        StreamTransactionOptions options = new StreamTransactionOptions()
                .allowImplicit(true)
                .writeCollections(writeCollections.toArray(new String[0]))
                .lockTimeout(Math.max(timeout, 0));
        streamTransaction = database.beginStreamTransaction(options);
        if (logger.isDebugEnabled()) {
            logger.debug("Began stream transaction " + streamTransaction.getId() + " writing collections " + writeCollections);
        }
        return createResource();
    }

    void commit() {
        if (streamTransaction != null && streamTransaction.getStatus() == StreamTransactionStatus.running) {
            database.commitStreamTransaction(streamTransaction.getId());
        }
    }

    void rollback() {
        if (streamTransaction != null && streamTransaction.getStatus() == StreamTransactionStatus.running) {
            database.abortStreamTransaction(streamTransaction.getId());
        }
    }

    @Override
    public boolean isRollbackOnly() {
        return streamTransaction != null && streamTransaction.getStatus() == StreamTransactionStatus.aborted;
    }

    @Override
    public void flush() {
        // nothing to do
    }

    @Override
    public String toString() {
        return streamTransaction == null ? "(not begun)" : streamTransaction.getId();
    }

    private void addCollections(Collection<String> collections) {
        if (streamTransaction != null) {
            if (!writeCollections.containsAll(collections)) {
                Set<String> additional = new HashSet<>(collections);
                additional.removeAll(writeCollections);
                throw new IllegalTransactionStateException("Stream transaction already started on collections " + writeCollections + ", no additional collections allowed: " + additional);
            }
        }
        writeCollections.addAll(collections);
    }
}
