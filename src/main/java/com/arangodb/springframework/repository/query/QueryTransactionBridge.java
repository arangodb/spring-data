package com.arangodb.springframework.repository.query;

import org.springframework.core.NamedInheritableThreadLocal;

import java.util.Collection;
import java.util.function.Function;

/**
 * Bridge to postpone late transaction start to be able to inject collections from query side.
 */
public class QueryTransactionBridge {
    private static final Function<Collection<String>, String> NO_TRANSACTION = any -> null;
    private static final ThreadLocal<Function<Collection<String>, String>> CURRENT_TRANSACTION_BEGIN = new NamedInheritableThreadLocal<>("ArangoTransactionBegin");

    public QueryTransactionBridge() {
        CURRENT_TRANSACTION_BEGIN.set(NO_TRANSACTION);
    }

    public void setCurrentTransactionBegin(Function<Collection<String>, String> begin) {
        CURRENT_TRANSACTION_BEGIN.set(begin);
    }

    public void clearCurrentTransactionBegin() {
        CURRENT_TRANSACTION_BEGIN.set(NO_TRANSACTION);
    }

    public String beginCurrentTransaction(Collection<String> collections) {
        return CURRENT_TRANSACTION_BEGIN.get().apply(collections);
    }
}
