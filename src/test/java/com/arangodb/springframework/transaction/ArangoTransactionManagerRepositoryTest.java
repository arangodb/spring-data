package com.arangodb.springframework.transaction;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTransactionalTestConfiguration;
import com.arangodb.springframework.repository.HumanBeingRepository;
import com.arangodb.springframework.testdata.HumanBeing;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = { ArangoTransactionalTestConfiguration.class })
public class ArangoTransactionManagerRepositoryTest extends AbstractArangoTest {

	private final HumanBeing anakin = new HumanBeing("Anakin", "Skywalker", false);

	@Autowired
	private HumanBeingRepository humanBeingRepository;

	@Before
	public void cleanupDatabase() {
		template.collection(HumanBeing.class).truncate();
	}

	@Test
	public void shouldWorkWithoutTransaction() {
		humanBeingRepository.save(anakin);

		assertThat(humanBeingRepository.findByNameAndSurname(anakin.getName(), anakin.getSurname())).isPresent();
	}

	@Test
	@Transactional
	public void shouldWorkWithinTransaction() {
		humanBeingRepository.save(anakin);

		assertThat(humanBeingRepository.findByNameAndSurname(anakin.getName(), anakin.getSurname())).isPresent();
	}

	@Test
	@Transactional
	public void shouldWorkAfterTransaction() {
		TestTransaction.flagForCommit();

		humanBeingRepository.save(anakin);

		assertThat(TestTransaction.isFlaggedForRollback()).isFalse();
		TestTransaction.end();

		assertThat(humanBeingRepository.findByNameAndSurname(anakin.getName(), anakin.getSurname())).isPresent();
	}

	@Test
	@Transactional
	public void shouldRollbackWithinTransaction() {
		humanBeingRepository.save(anakin);
		TestTransaction.flagForRollback();
		TestTransaction.end();

		assertThat(humanBeingRepository.findByNameAndSurname(anakin.getName(), anakin.getSurname())).isNotPresent();
	}
}
