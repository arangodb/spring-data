package com.arangodb.springframework.transaction;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTransactionalTestConfiguration;
import com.arangodb.springframework.repository.ActorRepository;
import com.arangodb.springframework.repository.MovieRepository;
import com.arangodb.springframework.testdata.Movie;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Streamable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = { ArangoTransactionalTestConfiguration.class })
class ArangoTransactionManagerRepositoryTest extends AbstractArangoTest {

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

	@Test
	@Transactional
	public void shouldFindSavedEntityWithinTransaction_findAllById() {
		Movie saved = movieRepository.save(starWars);

		List<Movie> list = Streamable.of(movieRepository.findAllById(List.of(saved.getId()))).toList();

		assertThat(list).hasSize(1);
		assertThat(list.getFirst().getId()).isEqualTo(saved.getId());
	}

	@Test
	@Transactional
	public void shouldFindSavedEntityWithinTransaction_findById() {
		Movie saved = movieRepository.save(starWars);

		Movie found = movieRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getId()).isEqualTo(saved.getId());
	}

	@Test
	@Transactional
	public void shouldFindSavedEntityWithinTransaction_existsById() {
		Movie saved = movieRepository.save(starWars);

		Boolean exists = movieRepository.existsById(saved.getId());

		assertThat(exists).isTrue();
	}

	@Test
	@Transactional
	public void shouldFindSavedEntityWithinTransaction_saveAll() {
		Movie saved = Streamable.of(movieRepository.saveAll(List.of(starWars))).stream().toList().getFirst();

		Boolean exists = movieRepository.existsById(saved.getId());

		assertThat(exists).isTrue();
	}

	@Test
	@Transactional
	public void shouldFindSavedEntityWithinTransaction_findAll() {
		Movie saved = movieRepository.save(starWars);

		List<Movie> list = Streamable.of(movieRepository.findAll()).toList();

		assertThat(list).hasSize(1);
		assertThat(list.getFirst().getId()).isEqualTo(saved.getId());
	}

	@Test
	@Transactional
	@Disabled("count is not transactional")
	public void shouldFindSavedEntityWithinTransaction_count() {
		movieRepository.save(starWars);

		long count = movieRepository.count();

		assertThat(count).isEqualTo(1);
	}

	@Test
	@Transactional
	public void shouldFindSavedEntityWithinTransaction_deleteById() {
		Movie saved = movieRepository.save(starWars);

		movieRepository.deleteById(saved.getId());
		Boolean exists = movieRepository.existsById(saved.getId());

		assertThat(exists).isFalse();
	}

	@Test
	@Transactional
	public void shouldFindSavedEntityWithinTransaction_delete() {
		Movie saved = movieRepository.save(starWars);

		movieRepository.delete(saved);
		Boolean exists = movieRepository.existsById(saved.getId());

		assertThat(exists).isFalse();
	}

	@Test
	@Transactional
	public void shouldFindSavedEntityWithinTransaction_deleteAllById() {
		Movie saved = movieRepository.save(starWars);

		movieRepository.deleteAllById(List.of(saved.getId()));
		Boolean exists = movieRepository.existsById(saved.getId());

		assertThat(exists).isFalse();
	}

	@Test
	@Transactional
	public void shouldFindSavedEntityWithinTransaction_deleteAll() {
		Movie saved = movieRepository.save(starWars);

		movieRepository.deleteAll(List.of(saved));
		Boolean exists = movieRepository.existsById(saved.getId());

		assertThat(exists).isFalse();
	}

	@Test
	@Transactional
	@Disabled("delete all is not transactional")
	public void shouldFindSavedEntityWithinTransaction_deleteAllNoArg() {
		Movie saved = movieRepository.save(starWars);

		movieRepository.deleteAll();
		Boolean exists = movieRepository.existsById(saved.getId());

		assertThat(exists).isFalse();
	}
}
