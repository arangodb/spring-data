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

package com.arangodb.springframework.core.convert.resolver;

import java.lang.annotation.Annotation;
import java.util.Optional;

import com.arangodb.ArangoDBException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.annotation.To;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.repository.query.QueryTransactionBridge;

public class DefaultResolverFactory implements ResolverFactory, ApplicationContextAware {

    private ObjectProvider<ArangoOperations> template;
    private ObjectProvider<QueryTransactionBridge> transactionBridge;

    @SuppressWarnings("unchecked")
    @Override
    public <A extends Annotation> Optional<ReferenceResolver<A>> getReferenceResolver(final A annotation) {
        try {
            if (annotation instanceof Ref) {
                return Optional.of((ReferenceResolver<A>) new RefResolver(template.getObject(), transactionBridge.getIfUnique()));
            }
        } catch (final Exception e) {
            throw ArangoDBException.of(e);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends Annotation> Optional<RelationResolver<A>> getRelationResolver(final A annotation,
                                                                                    final Class<? extends Annotation> collectionType) {
        try {
            if (annotation instanceof From) {
                if (collectionType == Edge.class) {
                    return Optional.of((RelationResolver<A>) new EdgeFromResolver(template.getObject(), transactionBridge.getIfUnique()));
                }
                return Optional.of((RelationResolver<A>) new DocumentFromResolver(template.getObject(), transactionBridge.getIfUnique()));
            }
            if (annotation instanceof To) {
                if (collectionType == Edge.class) {
                    return Optional.of((RelationResolver<A>) new EdgeToResolver(template.getObject(), transactionBridge.getIfUnique()));
                }
                return Optional.of((RelationResolver<A>) new DocumentToResolver(template.getObject(), transactionBridge.getIfUnique()));
            }
            if (annotation instanceof Relations) {
                return Optional.of((RelationResolver<A>) new RelationsResolver(template.getObject(), transactionBridge.getIfUnique()));
            }
        } catch (final Exception e) {
            throw ArangoDBException.of(e);
        }
        return Optional.empty();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        template = applicationContext.getBeanProvider(ArangoOperations.class);
        transactionBridge = applicationContext.getBeanProvider(QueryTransactionBridge.class);
    }
}
