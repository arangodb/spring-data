package com.arangodb.springframework.testdata.chess.repo;

import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.repository.ArangoRepository;
import com.arangodb.springframework.testdata.chess.entity.Tournament;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoPage;
import org.springframework.data.geo.Point;

import java.time.LocalDate;
import java.util.List;

public interface TournamentRepository extends ArangoRepository<Tournament, String> {
    List<Tournament> findAllByDateBetween(LocalDate start, LocalDate end, AqlQueryOptions opts);

    Iterable<Tournament> findByNameContainingIgnoreCase(String match, AqlQueryOptions opts);

    List<Tournament> findAllByLocationWithin(Point location, Distance distance, AqlQueryOptions opts);

    GeoPage<Tournament> findAllByLocationWithin(Pageable pageable, Point location, Distance distance, AqlQueryOptions opts);
}



