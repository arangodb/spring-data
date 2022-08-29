package com.arangodb.springframework.core.convert.resolver;

import java.lang.annotation.Annotation;
import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.arangodb.springframework.annotation.Document;
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
        ReferenceResolver<A> resolver = null;
        if (annotation instanceof Ref) {
            resolver = (ReferenceResolver<A>) new RefResolver(template.getObject(), transactionBridge.getIfUnique());
        }
        return Optional.ofNullable(resolver);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends Annotation> Optional<RelationResolver<A>> getRelationResolver(final A annotation,
                                                                                    final Class<? extends Annotation> collectionType) {
        RelationResolver<A> resolver = null;
        if (annotation instanceof From) {
            if (collectionType == Edge.class) {
                resolver = (RelationResolver<A>) new EdgeFromResolver(template.getObject(), transactionBridge.getIfUnique());
            } else if (collectionType == Document.class) {
                resolver = (RelationResolver<A>) new DocumentFromResolver(template.getObject(), transactionBridge.getIfUnique());
            }
        } else if (annotation instanceof To) {
            if (collectionType == Edge.class) {
                resolver = (RelationResolver<A>) new EdgeToResolver(template.getObject(), transactionBridge.getIfUnique());
            } else if (collectionType == Document.class) {
                resolver = (RelationResolver<A>) new DocumentToResolver(template.getObject(), transactionBridge.getIfUnique());
            }
        } else if (annotation instanceof Relations) {
            resolver = (RelationResolver<A>) new RelationsResolver(template.getObject(), transactionBridge.getIfUnique());
        }
        return Optional.ofNullable(resolver);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        template = applicationContext.getBeanProvider(ArangoOperations.class);
        transactionBridge = applicationContext.getBeanProvider(QueryTransactionBridge.class);
    }
}
