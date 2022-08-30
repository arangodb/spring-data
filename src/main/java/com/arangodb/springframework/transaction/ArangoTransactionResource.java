package com.arangodb.springframework.transaction;

import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class ArangoTransactionResource {

    private final String streamTransactionId;
    private final Set<String> collectionNames;

    ArangoTransactionResource(@Nullable String streamTransactionId, Collection<String> collectionNames) {
        this.streamTransactionId = streamTransactionId;
        this.collectionNames = new HashSet<>(collectionNames);
    }

    String getStreamTransactionId() {
        return streamTransactionId;
    }

    Set<String> getCollectionNames() {
        return collectionNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArangoTransactionResource that = (ArangoTransactionResource) o;
        return Objects.equals(streamTransactionId, that.streamTransactionId) && collectionNames.equals(that.collectionNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streamTransactionId);
    }
}
