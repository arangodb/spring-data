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
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.template.CollectionCallback;
import com.arangodb.springframework.repository.query.QueryTransactionBridge;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.*;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Function;

/**
 * Transaction manager using ArangoDB stream transactions on the
 * {@linkplain ArangoOperations#db() current database} of the
 * template. A {@linkplain ArangoTransactionObject transaction object} using
 * a shared {@linkplain ArangoTransactionHolder holder} is used for the
 * {@link DefaultTransactionStatus}. Neither
 * {@linkplain TransactionDefinition#getPropagationBehavior() propagation}
 * {@linkplain TransactionDefinition#PROPAGATION_NESTED nested} nor
 * {@linkplain TransactionDefinition#getIsolationLevel() isolation}
 * {@linkplain TransactionDefinition#ISOLATION_SERIALIZABLE serializable} are
 * supported.
 *
 * @author Arne Burmeister
 */
public class ArangoTransactionManager extends AbstractPlatformTransactionManager implements InitializingBean {

    private final ArangoOperations operations;
    private final QueryTransactionBridge bridge;

    public ArangoTransactionManager(ArangoOperations operations, QueryTransactionBridge bridge) {
        this.operations = operations;
        this.bridge = bridge;
        setGlobalRollbackOnParticipationFailure(true);
        setTransactionSynchronization(SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
    }

    /**
     * Check for supported property settings.
     */
    @Override
    public void afterPropertiesSet() {
        if (isNestedTransactionAllowed()) {
            throw new IllegalStateException("Nested transactions must not be allowed");
        }
        if (!isGlobalRollbackOnParticipationFailure()) {
            throw new IllegalStateException("Global rollback on participating failure is required");
        }
        if (getTransactionSynchronization() == SYNCHRONIZATION_NEVER) {
            throw new IllegalStateException("Transaction synchronization must not be disabled");
        }
    }

    /**
     * Creates a new transaction object. Any holder bound will be reused.
     */
    @Override
    protected ArangoTransactionObject doGetTransaction() {
        ArangoTransactionHolder holder = (ArangoTransactionHolder) TransactionSynchronizationManager.getResource(this);
        try {
            return new ArangoTransactionObject(operations.db(), CollectionCallback.fromOperations(operations), getDefaultTimeout(), holder);
        } catch (ArangoDBException error) {
            throw new TransactionSystemException("Cannot create transaction object", error);
        }
    }

    /**
     * Connect the new transaction object to the query bridge.
     *
     * @see QueryTransactionBridge#setCurrentTransaction(Function)
     * @see #prepareSynchronization(DefaultTransactionStatus, TransactionDefinition)
     * @throws InvalidIsolationLevelException for {@link TransactionDefinition#ISOLATION_SERIALIZABLE}
     */
    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) throws InvalidIsolationLevelException {
        int isolationLevel = definition.getIsolationLevel();
        if (isolationLevel != TransactionDefinition.ISOLATION_DEFAULT && (isolationLevel & TransactionDefinition.ISOLATION_SERIALIZABLE) != 0) {
            throw new InvalidIsolationLevelException("ArangoDB does not support isolation level serializable");
        }
        ArangoTransactionObject tx = (ArangoTransactionObject) transaction;
        bridge.setCurrentTransaction(collections -> {
            try {
                return tx.getOrBegin(collections).getStreamTransactionId();
            } catch (ArangoDBException error) {
                throw new TransactionSystemException("Cannot begin transaction", error);
            }
        });
    }

    /**
     * Commit the current stream transaction. The query bridge is cleared
     * afterwards.
     *
     * @see ArangoDatabase#commitStreamTransaction(String)
     * @see QueryTransactionBridge#clearCurrentTransaction()
     */
    @Override
    protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
        ArangoTransactionObject tx = (ArangoTransactionObject) status.getTransaction();
        if (logger.isDebugEnabled()) {
            logger.debug("Commit stream transaction " + tx);
        }
        try {
            tx.commit();
            afterCompletion();
        } catch (ArangoDBException error) {
            if (!isRollbackOnCommitFailure()) {
                try {
                    tx.rollback();
                } catch (ArangoDBException noRollback) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Cannot rollback after commit " + tx, noRollback);
                    }
                    // expose commit exception instead
                } finally {
                    afterCompletion();
                }
            }
            throw new TransactionSystemException("Cannot commit transaction " + tx, error);
        }
    }

    /**
     * Roll back the current stream transaction. The query bridge is cleared
     * afterwards.
     *
     * @see ArangoDatabase#abortStreamTransaction(String)
     * @see QueryTransactionBridge#clearCurrentTransaction()
     */
    @Override
    protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
        ArangoTransactionObject tx = (ArangoTransactionObject) status.getTransaction();
        if (logger.isDebugEnabled()) {
            logger.debug("Rollback stream transaction " + tx);
        }
        try {
            tx.rollback();
        } catch (ArangoDBException error) {
            throw new TransactionSystemException("Cannot roll back transaction " + tx, error);
        } finally {
            afterCompletion();
        }
    }

    /**
     * Check if the transaction object has the bound holder. For new
     * transactions the holder will be bound afterwards.
     */
    @Override
    protected boolean isExistingTransaction(Object transaction) throws TransactionException {
        ArangoTransactionHolder holder = ((ArangoTransactionObject) transaction).getHolder();
        return holder == TransactionSynchronizationManager.getResource(this);
    }

    /**
     * Mark the transaction as global rollback only.
     *
     * @see #isGlobalRollbackOnParticipationFailure()
     */
    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
        ArangoTransactionObject tx = (ArangoTransactionObject) status.getTransaction();
        tx.getHolder().setRollbackOnly();
    }

    /**
     * Any transaction object is configured according to the definition upfront.
     * Bind the holder for the first new transaction created.
     *
     * @see ArangoTransactionHolder
     */
    @Override
    protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
        ArangoTransactionObject transaction = status.hasTransaction() ? (ArangoTransactionObject) status.getTransaction() : null;
        if (transaction != null) {
            transaction.configure(definition);
        }
        super.prepareSynchronization(status, definition);
        if (transaction != null && status.isNewSynchronization()) {
            ArangoTransactionHolder holder = transaction.getHolder();
            TransactionSynchronizationManager.bindResource(this, holder);
        }
    }

    private void afterCompletion() {
        bridge.clearCurrentTransaction();
        TransactionSynchronizationManager.unbindResource(this);
    }
}
