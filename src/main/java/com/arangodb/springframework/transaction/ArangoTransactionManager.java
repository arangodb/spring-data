package com.arangodb.springframework.transaction;

import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.DbName;
import com.arangodb.model.StreamTransactionOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.repository.query.QueryTransactionBridge;
import org.springframework.transaction.*;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
        setNestedTransactionAllowed(false);
        setTransactionSynchronization(SYNCHRONIZATION_ALWAYS);
        setValidateExistingTransaction(true);
        setGlobalRollbackOnParticipationFailure(true);
        setRollbackOnCommitFailure(true);
    }

    /**
     * Creates a new transaction object. Any synchronized resource will be reused.
     */
    @Override
    protected Object doGetTransaction() {
        DbName database = operations.getDatabaseName();
        if (logger.isDebugEnabled()) {
            logger.debug("Create new transaction for database " + database);
        }
        try {
            ArangoTransactionResource resource = (ArangoTransactionResource) TransactionSynchronizationManager.getResource(database);
            return new ArangoTransactionObject(operations.driver().db(database), getDefaultTimeout(), resource);
        } catch (ArangoDBException error) {
            throw new TransactionSystemException("Cannot create transaction object", error);
        }
    }

    /**
     * Configures the new transaction object. The resulting resource will be synchronized and the bridge will be initialized.
     *
     * @see QueryTransactionBridge
     */
    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionUsageException {
        int isolationLevel = definition.getIsolationLevel();
        if (isolationLevel != -1 && (isolationLevel & TransactionDefinition.ISOLATION_SERIALIZABLE) != 0) {
            throw new InvalidIsolationLevelException("ArangoDB does not support isolation level serializable");
        }
        ArangoTransactionObject tx = (ArangoTransactionObject) transaction;
        tx.configure(definition);
        DbName key = operations.getDatabaseName();
        TransactionSynchronizationManager.unbindResourceIfPossible(key);
        TransactionSynchronizationManager.bindResource(key, tx.getResource());
        bridge.setCurrentTransaction(collections -> tx.getOrBegin(collections).getStreamTransactionId());
    }

    /**
     * Commit the current stream transaction iff any. The bridge is cleared afterwards.
     */
    @Override
    protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
        ArangoTransactionObject tx = (ArangoTransactionObject) status.getTransaction();
        if (logger.isDebugEnabled()) {
            logger.debug("Commit stream transaction " + tx);
        }
        try {
            tx.commit();
            bridge.clearCurrentTransaction();
        } catch (ArangoDBException error) {
            throw new TransactionSystemException("Cannot commit transaction " + tx, error);
        }
    }

    /**
     * Roll back the current stream transaction iff any. The bridge is cleared afterwards.
     */
    @Override
    protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
        ArangoTransactionObject tx = (ArangoTransactionObject) status.getTransaction();
        if (logger.isDebugEnabled()) {
            logger.debug("Rollback stream transaction " + tx);
        }
        try {
            tx.rollback();
            bridge.clearCurrentTransaction();
        } catch (ArangoDBException error) {
            throw new TransactionSystemException("Cannot roll back transaction " + tx, error);
        }
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) throws TransactionException {
        return transaction instanceof ArangoTransactionObject && ((ArangoTransactionObject) transaction).exists();
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
        ArangoTransactionObject tx = (ArangoTransactionObject) status.getTransaction();
        tx.setRollbackOnly();
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        TransactionSynchronizationManager.unbindResourceIfPossible(operations.getDatabaseName());
    }
}
