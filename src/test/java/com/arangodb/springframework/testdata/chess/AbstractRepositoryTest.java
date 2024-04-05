package com.arangodb.springframework.testdata.chess;

import com.arangodb.springframework.AbstractTxTest;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.testdata.chess.entity.Player;
import com.arangodb.springframework.testdata.chess.entity.Score;
import com.arangodb.springframework.testdata.chess.entity.Tournament;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;

import java.time.LocalDate;
import java.util.List;

abstract class AbstractRepositoryTest extends AbstractTxTest {

    @Autowired
    private ArangoOperations ops;

    protected List<Player> players = List.of(
            new Player("Magnus Carlsen", 2830, "Norway"),
            new Player("Fabiano Caruana", 2803, "US"),
            new Player("Maxime Vachier-Lagrave", 2732, "France"),
            new Player("Hikaru Nakamura", 2789, "US"),
            new Player("Ding Liren", 2762, "China"),
            new Player("Wesley So", 2757, "US"),
            new Player("Alireza Firouzja", 2760, "France"),
            new Player("Anish Giri", 2745, "Netherlands"),
            new Player("Ian Nepomniachtchi", 2758, "Russia")
    );

    protected List<Tournament> tournaments = List.of(
            new Tournament(
                    "Tata Steel 2023",
                    LocalDate.of(2023, 1, 13),
                    "Wijk aan Zee",
                    new Point(4.6, 52.5)
            ),
            new Tournament(
                    "World Chess Championship 2023",
                    LocalDate.of(2023, 4, 9),
                    "Astana",
                    new Point(71.422222, 51.147222)
            ),
            new Tournament(
                    "Norway Chess 2023",
                    LocalDate.of(2023, 5, 30),
                    "Stavanger",
                    new Point(5.731389, 58.97)
            )
    );

    protected List<Score> scores = List.of(
            new Score(players.get(7), tournaments.get(0), 1),
            new Score(players.get(0), tournaments.get(0), 3),
            new Score(players.get(5), tournaments.get(0), 4),
            new Score(players.get(1), tournaments.get(0), 5),
            new Score(players.get(4), tournaments.get(1), 1),
            new Score(players.get(8), tournaments.get(1), 2),
            new Score(players.get(3), tournaments.get(1), 4),
            new Score(players.get(1), tournaments.get(1), 5),
            new Score(players.get(6), tournaments.get(1), 6),
            new Score(players.get(3), tournaments.get(2), 1),
            new Score(players.get(1), tournaments.get(2), 2),
            new Score(players.get(7), tournaments.get(2), 4),
            new Score(players.get(5), tournaments.get(2), 5),
            new Score(players.get(0), tournaments.get(2), 6)
    );

    protected AbstractRepositoryTest(boolean withinTx) {
        super(withinTx, Player.class, Score.class, Tournament.class);
    }

    @BeforeEach
    void importData() {
        ops.insertAll(players, insertOpts, Player.class);
        ops.insertAll(tournaments, insertOpts, Tournament.class);
        ops.insertAll(scores, insertOpts, Score.class);
    }

}
