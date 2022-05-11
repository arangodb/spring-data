package com.arangodb.springframework.transaction;

import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.repository.query.QueryTransactionBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

public class ArangoTransactionManagementConfigurer implements TransactionManagementConfigurer {

    private final ArangoOperations operations;
    private final QueryTransactionBridge bridge;

    public ArangoTransactionManagementConfigurer(ArangoOperations operations, QueryTransactionBridge bridge) {
        this.operations = operations;
        this.bridge = bridge;
    }

    @Override
    @Bean
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return new ArangoTransactionManager(operations, bridge);
    }
}
