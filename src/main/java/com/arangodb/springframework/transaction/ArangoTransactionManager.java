package com.arangodb.springframework.transaction;

import com.arangodb.ArangoDatabase;
import com.arangodb.model.StreamTransactionOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.repository.query.QueryTransactionBridge;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.Collection;
import java.util.function.Function;

/**
 * Transaction manager using ArangoDB stream transactions on the
 * {@linkplain ArangoOperations#getDatabaseName()} current database} of the template.
 * Isolation level {@linkplain TransactionDefinition#ISOLATION_SERIALIZABLE serializable} is not supported.
 *
 * @see ArangoDatabase#beginStreamTransaction(StreamTransactionOptions)
 */
public class ArangoTransactionManager extends AbstractPlatformTransactionManager {

    private final ArangoOperations operations;
    private final QueryTransactionBridge bridge;

    public ArangoTransactionManager(ArangoOperations operations, QueryTransactionBridge bridge) {
        this.operations = operations;
        this.bridge = bridge;
    }

    @Override
    protected Object doGetTransaction() throws TransactionException {
        return new ArangoTransaction(operations.driver().db(operations.getDatabaseName()));
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) throws InvalidIsolationLevelException {
        int isolationLevel = definition.getIsolationLevel();
        if (isolationLevel != -1 && (isolationLevel & TransactionDefinition.ISOLATION_SERIALIZABLE) != 0) {
            throw new InvalidIsolationLevelException("ArangoDB does not support isolation level serializable");
        }
        ArangoTransaction tx = (ArangoTransaction) transaction;
        tx.configure(definition);
        Function<Collection<String>, String> begin = tx::begin;
        bridge.setCurrentTransactionBegin(begin.andThen(id -> {
            if (logger.isDebugEnabled()) {
                logger.debug("Began stream transaction " + id);
            }
            return id;
        }));
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
        ArangoTransaction tx = (ArangoTransaction) status.getTransaction();
        if (logger.isDebugEnabled()) {
            logger.debug("Commit stream transaction " + tx);
        }
        tx.commit();
        bridge.clearCurrentTransactionBegin();
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
        ArangoTransaction tx = (ArangoTransaction) status.getTransaction();
        if (logger.isDebugEnabled()) {
            logger.debug("Rollback stream transaction " + tx);
        }
        tx.rollback();
        bridge.clearCurrentTransactionBegin();
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) throws TransactionException {
        return transaction instanceof ArangoTransaction
                && ((ArangoTransaction) transaction).exists();
    }
}
