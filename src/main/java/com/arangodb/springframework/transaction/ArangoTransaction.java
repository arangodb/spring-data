package com.arangodb.springframework.transaction;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.StreamTransactionEntity;
import com.arangodb.entity.StreamTransactionStatus;
import com.arangodb.model.StreamTransactionOptions;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.support.SmartTransactionObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class ArangoTransaction implements SmartTransactionObject {

    private final ArangoDatabase database;
    private TransactionDefinition definition;
    private StreamTransactionEntity transaction;

    ArangoTransaction(ArangoDatabase database) {
        this.database = database;
    }

    boolean exists() {
        return transaction != null;
    }

    void configure(TransactionDefinition definition) {
        this.definition = definition;
    }

    String begin(Collection<String> collections) {
        if (transaction != null) {
            throw new IllegalTransactionStateException("Stream transaction already started");
        }
        Set<String> allCollections = new HashSet<>(collections);
        if (definition instanceof TransactionAttribute) {
            allCollections.addAll(((TransactionAttribute) definition).getLabels());
        }
        StreamTransactionOptions options = new StreamTransactionOptions().allowImplicit(true)
                .writeCollections(allCollections.toArray(new String[0]))
                .lockTimeout(definition.getTimeout() == -1 ? 0 : definition.getTimeout());
        transaction = database.beginStreamTransaction(options);
        return transaction.getId();
    }

    void commit() {
        database.commitStreamTransaction(transaction.getId());
    }

    void rollback() {
        database.abortStreamTransaction(transaction.getId());
    }

    @Override
    public boolean isRollbackOnly() {
        return transaction != null && transaction.getStatus() == StreamTransactionStatus.aborted;
    }

    @Override
    public void flush() {
        // nothing to do
    }

    @Override
    public String toString() {
        return transaction == null ? "(not begun)" : transaction.getId();
    }
}
