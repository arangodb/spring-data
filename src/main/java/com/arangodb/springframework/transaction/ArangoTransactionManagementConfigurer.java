package com.arangodb.springframework.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.repository.query.QueryTransactionBridge;

/**
 * To enable stream transactions for Arango Spring Data, create a
 * {@link org.springframework.context.annotation.Configuration} class annotated with
 * {@link org.springframework.transaction.annotation.EnableTransactionManagement} and
 * {@link org.springframework.context.annotation.Import} this one.
 */
public class ArangoTransactionManagementConfigurer implements TransactionManagementConfigurer {

    @Autowired
    private ArangoOperations operations;
    private final QueryTransactionBridge bridge = new QueryTransactionBridge();

    @Override
    @Bean
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return new ArangoTransactionManager(operations, bridge);
    }

    @Bean
    QueryTransactionBridge arangoQueryTransactionBridge() {
        return bridge;
    }
}
