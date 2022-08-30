package com.arangodb.springframework.transaction;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Synchronisation resource (has to be mutable).
 *
 * @see TransactionSynchronizationManager#bindResource(Object, Object)
 * @see ArangoTransactionObject
 */
class ArangoTransactionResource {

    private String streamTransactionId;
    private Set<String> collectionNames;

    private boolean rollbackOnly;
    int references = 0;

    ArangoTransactionResource(@Nullable String streamTransactionId, Set<String> collectionNames, boolean rollbackOnly) {
        this.streamTransactionId = streamTransactionId;
        setCollectionNames(collectionNames);
        this.rollbackOnly = rollbackOnly;
    }

    String getStreamTransactionId() {
        return streamTransactionId;
    }

    void setStreamTransactionId(String streamTransactionId) {
        this.streamTransactionId = streamTransactionId;
    }

    Set<String> getCollectionNames() {
        return collectionNames;
    }

    void setCollectionNames(Set<String> collectionNames) {
        this.collectionNames = new HashSet<>(collectionNames);
    }

    boolean isRollbackOnly() {
        return rollbackOnly;
    }

    void setRollbackOnly(boolean rollbackOnly) {
        this.rollbackOnly = rollbackOnly;
    }

    void increaseReferences() {
        ++references;
    }

    boolean isSingleReference() {
        return references <= 1;
    }

    void decreasedReferences() {
        --references;
    }
}
