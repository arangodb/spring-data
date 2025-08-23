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

import org.springframework.lang.Nullable;
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
 * @author Arne Burmeister
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
        if (transactionDefinition instanceof TransactionAttribute transactionAttribute) {
            setQualifier(transactionAttribute.getQualifier());
            setLabels(transactionAttribute.getLabels());
            setRollbackOn(transactionAttribute::rollbackOn);
        }
    }

    @Override
    @Nullable
    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(@Nullable String qualifier) {
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
