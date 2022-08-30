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
 */
public class ArangoTransactionManager extends AbstractPlatformTransactionManager {

    private final ArangoOperations operations;
    private final QueryTransactionBridge bridge;

    public ArangoTransactionManager(ArangoOperations operations, QueryTransactionBridge bridge) {
        this.operations = operations;
        this.bridge = bridge;
        setValidateExistingTransaction(true);
    }

    /**
     * Creates a new transaction object. Any synchronized resource will be reused.
     */
    @Override
    protected ArangoTransactionObject doGetTransaction() {
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
     * @see ArangoDatabase#beginStreamTransaction(StreamTransactionOptions)
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
        bridge.setCurrentTransaction(collections -> {
            try {
                return tx.getOrBegin(collections).getStreamTransactionId();
            } catch (ArangoDBException error) {
                throw new TransactionSystemException("Cannot begin transaction", error);
            }
        });
    }

    /**
     * Commit the current stream transaction iff any. The bridge is cleared afterwards.
     *
     * @see ArangoDatabase#commitStreamTransaction(String)
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
     *
     * @see ArangoDatabase#abortStreamTransaction(String)
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

    /**
     * Check if the transaction objects has an underlying stream transaction.
     *
     * @see ArangoDatabase#getStreamTransaction(String)
     */
    @Override
    protected boolean isExistingTransaction(Object transaction) throws TransactionException {
        return ((ArangoTransactionObject) transaction).exists();
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
        ArangoTransactionObject tx = (ArangoTransactionObject) status.getTransaction();
        tx.setRollbackOnly();
    }

    @Override
    protected DefaultTransactionStatus newTransactionStatus(TransactionDefinition definition, Object transaction, boolean newTransaction, boolean newSynchronization, boolean debug, Object suspendedResources) {
        return super.newTransactionStatus(definition, transaction, newTransaction, newSynchronization, debug, suspendedResources);
    }

    /**
     * Bind the resource for the first new transaction created.
     */
    @Override
    protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
        super.prepareSynchronization(status, definition);
        if (status.isNewTransaction()) {
            ArangoTransactionResource resource = ((ArangoTransactionObject) status.getTransaction()).getResource();
            resource.increaseReferences();
            if (resource.isSingleReference()) {
                TransactionSynchronizationManager.bindResource(operations.getDatabaseName(), resource);
            }
        }
    }

    /**
     * Unbind the resource for the last transaction completed.
     */
    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        ArangoTransactionResource resource = ((ArangoTransactionObject) transaction).getResource();
        if (resource.isSingleReference()) {
            TransactionSynchronizationManager.unbindResource(operations.getDatabaseName());
        }
        resource.decreasedReferences();
    }

}
