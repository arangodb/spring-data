package com.arangodb.springframework.core.mapping;

import com.arangodb.RequestContext;

import java.util.Objects;
import java.util.Optional;

public class TransactionMappingContext {
    public static TransactionMappingContext EMPTY = new TransactionMappingContext((String) null);

    private final String streamTransactionId;

    private TransactionMappingContext(final String streamTransactionId) {
        this.streamTransactionId = streamTransactionId;
    }

    public TransactionMappingContext(final RequestContext ctx) {
        this(ctx.getStreamTransactionId().orElse(null));
    }

    public Optional<String> getStreamTransactionId() {
        return Optional.ofNullable(streamTransactionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionMappingContext that = (TransactionMappingContext) o;
        return Objects.equals(streamTransactionId, that.streamTransactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(streamTransactionId);
    }

    @Override
    public String toString() {
        return "TransactionMappingContext{" +
                "streamTransactionId='" + streamTransactionId + '\'' +
                '}';
    }
}
