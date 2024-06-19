package com.arangodb.springframework.testdata.chess.entity;

import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;

@Edge
public record Score(
        @From
        Player player,
        @To
        Tournament tournament,
        int rank
) {
}
