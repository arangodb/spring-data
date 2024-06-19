package com.arangodb.springframework.testdata.chess;

import com.arangodb.springframework.testdata.chess.entity.*;
import com.arangodb.springframework.testdata.chess.repo.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

abstract class PlayerRepositoryAbstract extends AbstractRepositoryTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    PlayerRepository repo;

    @Test
    void findAllByCountry() {
        List<Player> expected = players.stream()
                .filter(it -> "US".equals(it.getCountry()))
                .toList();
        Iterable<Player> found = repo.findAllByCountry("US");
        assertThat(found).containsExactlyInAnyOrderElementsOf(expected);
        found.forEach(this::checkRefs);
    }

    @Test
    void findAllByRatingGreaterThan() {
        int rating = 2780;
        List<Player> expected = players.stream()
                .filter(it -> it.getRating() > rating)
                .sorted(Comparator.comparingInt(Player::getRating).reversed())
                .toList();

        for (int i = 0; i < expected.size(); i++) {
            Page<Player> page = repo.findAllByRatingGreaterThanOrderByRatingDesc(PageRequest.of(i, 1), rating);
            assertThat(page.getTotalElements()).isEqualTo(expected.size());
            assertThat(page.getTotalPages()).isEqualTo(expected.size());
            Player current = page.iterator().next();
            assertThat(current).isEqualTo(expected.get(i));
            checkRefs(current);
        }
    }

    private void checkRefs(Player p) {
        List<Score> expectedScores = scores.stream()
                .filter(it -> it.player().equals(p))
                .toList();
        assertThat(p.getScores()).containsExactlyInAnyOrderElementsOf(expectedScores);

        List<Tournament> expectedTournaments = expectedScores.stream()
                .map(Score::tournament)
                .toList();
        assertThat(p.getTournaments()).containsExactlyInAnyOrderElementsOf(expectedTournaments);
    }

}
