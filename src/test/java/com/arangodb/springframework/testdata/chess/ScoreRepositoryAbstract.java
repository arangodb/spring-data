package com.arangodb.springframework.testdata.chess;

import com.arangodb.springframework.testdata.chess.repo.ScoreRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

abstract class ScoreRepositoryAbstract extends AbstractRepositoryTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    ScoreRepository repo;

    ScoreRepositoryAbstract(boolean withinTx) {
        super(withinTx);
    }

    @Test
    @Disabled("BTS-1859")
    void findAll() {
        assertThat(repo.findAll(queryOpts))
                .hasSize(scores.size())
                .containsExactlyInAnyOrderElementsOf(scores);
    }

}
