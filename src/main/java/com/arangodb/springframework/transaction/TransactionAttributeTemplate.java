package com.arangodb.springframework.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

/**
 * Template class that simplifies programmatic transaction demarcation and
 * transaction exception handling in combination with a transaction manager using labels.
 *
 * @see ArangoTransactionManager
 */
public class TransactionAttributeTemplate extends TransactionTemplate implements TransactionAttribute {

    private static final Predicate<Throwable> DEFAULT_ROLLBACK_ON = ex -> (ex instanceof RuntimeException || ex instanceof Error);

    private String qualifier;
    private Collection<String> labels = Collections.emptyList();
    private Predicate<Throwable> rollbackOn = DEFAULT_ROLLBACK_ON;

    public TransactionAttributeTemplate() {
    }

    public TransactionAttributeTemplate(PlatformTransactionManager transactionManager) {
        super(transactionManager);
    }

    public TransactionAttributeTemplate(PlatformTransactionManager transactionManager, TransactionDefinition transactionDefinition) {
        super(transactionManager, transactionDefinition);
        if (transactionDefinition instanceof TransactionAttribute) {
            TransactionAttribute transactionAttribute = (TransactionAttribute) transactionDefinition;
            setQualifier(transactionAttribute.getQualifier());
            setLabels(transactionAttribute.getLabels());
            setRollbackOn(transactionAttribute::rollbackOn);
        }
    }

    @Override
    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    @Override
    public Collection<String> getLabels() {
        return labels;
    }

    public void setLabels(Collection<String> labels) {
        this.labels = labels;
    }

    @Override
    public boolean rollbackOn(Throwable ex) {
        return rollbackOn.test(ex);
    }

    public void setRollbackOn(Predicate<Throwable> rollbackOn) {
        this.rollbackOn = rollbackOn;
    }
}
