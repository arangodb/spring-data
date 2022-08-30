package com.arangodb.springframework.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.Collections;

public class TransactionAttributeTemplate extends TransactionTemplate implements TransactionAttribute {

    private String qualifier;
    private Collection<String> labels = Collections.emptyList();

    public TransactionAttributeTemplate(PlatformTransactionManager transactionManager) {
        super(transactionManager);
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
        return (ex instanceof RuntimeException || ex instanceof Error);
    }
}
