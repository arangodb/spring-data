package com.arangodb.springframework;

import com.arangodb.springframework.repository.query.QueryTransactionBridge;
import com.arangodb.springframework.transaction.ArangoTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@TestExecutionListeners(TransactionalTestExecutionListener.class)
@Import(ArangoTransactionManager.class)
public class ArangoTransactionalTestConfiguration extends ArangoTestConfiguration {

	@Bean
	public QueryTransactionBridge queryTransactionBridge() {
		return new QueryTransactionBridge();
	}
}
