package com.arangodb.springframework.testdata.chess.repo;

import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.repository.ArangoRepository;
import com.arangodb.springframework.testdata.chess.entity.Score;

public interface ScoreRepository extends ArangoRepository<Score, String> {
    @Query("FOR d IN #collection RETURN d")
    Iterable<Score> findAll(AqlQueryOptions opts);
}
