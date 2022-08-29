package com.arangodb.springframework;

import com.arangodb.springframework.repository.query.QueryTransactionBridge;
import com.arangodb.springframework.transaction.ArangoTransactionManagementConfigurer;
import com.arangodb.springframework.transaction.ArangoTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@TestExecutionListeners(TransactionalTestExecutionListener.class)
@Import(ArangoTransactionManagementConfigurer.class)
public class ArangoTransactionalTestConfiguration extends ArangoTestConfiguration {

}
