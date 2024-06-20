package com.arangodb.springframework.testdata.chess;

import com.arangodb.springframework.testdata.chess.entity.Player;
import com.arangodb.springframework.testdata.chess.entity.Score;
import com.arangodb.springframework.testdata.chess.entity.Tournament;
import com.arangodb.springframework.testdata.chess.repo.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.geo.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

abstract class TournamentRepositoryAbstract extends AbstractRepositoryTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    TournamentRepository repo;

    // equirectangular approximation
    static Distance calculateDistance(Point p1, Point p2) {
        double lat1Rad = Math.toRadians(p1.getY());
        double lat2Rad = Math.toRadians(p2.getY());
        double lon1Rad = Math.toRadians(p1.getX());
        double lon2Rad = Math.toRadians(p2.getX());

        double x = (lon2Rad - lon1Rad) * Math.cos((lat1Rad + lat2Rad) / 2);
        double y = (lat2Rad - lat1Rad);
        double distance = Math.sqrt(x * x + y * y) * 6371;

        return new Distance(distance, Metrics.KILOMETERS);
    }

    @Test
    void findAllByDateBetween() {
        var start = LocalDate.of(2023, 1, 1);
        var end = LocalDate.of(2023, 5, 1);
        List<Tournament> expected = tournaments.stream()
                .filter(it -> it.getDate().isAfter(start))
                .filter(it -> it.getDate().isBefore(end))
                .toList();
        List<Tournament> found = repo.findAllByDateBetween(start, end);
        assertThat(found)
                .hasSize(expected.size())
                .containsExactlyInAnyOrderElementsOf(expected);
        found.forEach(this::checkRefs);
    }

    @Test
    void findByNameContainingIgnoreCase() {
        String match = "2023";
        List<Tournament> expected = tournaments.stream()
                .filter(it -> it.getName().contains(match))
                .toList();
        Iterable<Tournament> found = repo.findByNameContainingIgnoreCase(match);
        assertThat(found)
                .hasSize(expected.size())
                .containsExactlyInAnyOrderElementsOf(expected);
        found.forEach(this::checkRefs);
    }

    @Test
    public void findAllByLocationWithin() {
        Point p = new Point(4.893611, 52.372778); // Amsterdam
        Distance d = new Distance(1_000, Metrics.KILOMETERS);

        List<Tournament> expected = tournaments.stream()
                .filter(it -> calculateDistance(p, it.getLocation()).compareTo(d) < 0)
                .toList();

        List<Tournament> found = repo.findAllByLocationWithin(p, d);
        assertThat(found)
                .hasSize(expected.size())
                .containsExactlyInAnyOrderElementsOf(expected);
        found.forEach(this::checkRefs);
    }

    @Test
    public void findAllByLocationWithinPageable() {
        Point p = new Point(4.893611, 52.372778); // Amsterdam
        Distance d = new Distance(1_000, Metrics.KILOMETERS);

        Map<Tournament, Distance> distances = tournaments.stream()
                .collect(toMap(Function.identity(), t -> calculateDistance(p, t.getLocation())));
        List<Tournament> expected = tournaments.stream()
                .filter(it -> distances.get(it).compareTo(d) < 0)
                .toList();

        for (int i = 0; i < expected.size(); i++) {
            GeoPage<Tournament> page = repo.findAllByLocationWithin(PageRequest.of(i, 1), p, d);
            assertThat(page.getTotalElements()).isEqualTo(expected.size());
            assertThat(page.getTotalPages()).isEqualTo(expected.size());
            GeoResult<Tournament> current = page.iterator().next();
            double expectedDistKm = distances.get(current.getContent()).in(Metrics.KILOMETERS).getValue();
            assertThat(current.getContent()).isEqualTo(expected.get(i));
            assertThat(current.getDistance().in(Metrics.KILOMETERS).getValue()).isCloseTo(expectedDistKm, withinPercentage(.01));
            assertThat(page.getAverageDistance().in(Metrics.KILOMETERS).getValue()).isCloseTo(expectedDistKm, withinPercentage(.01));
            checkRefs(current.getContent());
        }
    }

    private void checkRefs(Tournament t) {
        List<Score> expectedScores = scores.stream()
                .filter(it -> it.tournament().equals(t))
                .toList();
        assertThat(t.getStandings()).containsExactlyInAnyOrderElementsOf(expectedScores);

        List<Player> expectedPlayers = expectedScores.stream()
                .map(Score::player)
                .toList();
        assertThat(t.getPlayers()).containsExactlyInAnyOrderElementsOf(expectedPlayers);
    }

}
