package com.arangodb.springframework.testdata.chess.repo;

import com.arangodb.springframework.repository.ArangoRepository;
import com.arangodb.springframework.testdata.chess.entity.Score;

public interface ScoreRepository extends ArangoRepository<Score, String> {
}
