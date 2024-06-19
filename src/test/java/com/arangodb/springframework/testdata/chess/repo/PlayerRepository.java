package com.arangodb.springframework.testdata.chess.repo;

import com.arangodb.springframework.repository.ArangoRepository;
import com.arangodb.springframework.testdata.chess.entity.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlayerRepository extends ArangoRepository<Player, String> {
    Iterable<Player> findAllByCountry(String country);

    Page<Player> findAllByRatingGreaterThanOrderByRatingDesc(Pageable pageable, int rating);
}



