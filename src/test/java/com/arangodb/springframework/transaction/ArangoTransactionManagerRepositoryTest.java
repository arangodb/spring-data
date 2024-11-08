package com.arangodb.springframework.transaction;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTransactionalTestConfiguration;
import com.arangodb.springframework.repository.ActorRepository;
import com.arangodb.springframework.repository.MovieRepository;
import com.arangodb.springframework.testdata.Movie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = { ArangoTransactionalTestConfiguration.class })
public class ArangoTransactionManagerRepositoryTest extends AbstractArangoTest {

	public ArangoTransactionManagerRepositoryTest() {
		super(Movie.class);
	}

	@Autowired
	private MovieRepository movieRepository;
	@Autowired
	private ActorRepository actorRepository;

	Movie starWars = new Movie();

	{
		starWars.setName("Star Wars");
	}

	@Test
	public void shouldWorkWithoutTransaction() {
		movieRepository.save(starWars);

		assertThat(movieRepository.findById(starWars.getId())).isPresent();
	}

	@Test
	@Transactional
	public void shouldWorkWithinTransaction() {
		movieRepository.save(starWars);

		assertThat(movieRepository.findById(starWars.getId())).isPresent();
	}

	@Test
	@Transactional
	public void shouldWorkAfterTransaction() {
		TestTransaction.flagForCommit();

		movieRepository.save(starWars);
		TestTransaction.end();

		assertThat(movieRepository.findById(starWars.getId())).isPresent();
	}

	@Test
	@Transactional
	public void shouldRollbackWithinTransaction() {
		movieRepository.save(starWars);
		TestTransaction.end();

		assertThat(movieRepository.findById(starWars.getId())).isNotPresent();
	}

	@Test
	@Transactional(label = "actors")
	public void shouldCreateCollectionsBeforeTransaction() {
		actorRepository.findAll();
	}
}
