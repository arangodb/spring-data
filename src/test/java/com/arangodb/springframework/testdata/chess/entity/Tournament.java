package com.arangodb.springframework.testdata.chess.entity;

import com.arangodb.springframework.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.geo.Point;

import java.time.LocalDate;
import java.util.List;

@Document
@Data
@RequiredArgsConstructor
public class Tournament {
    private @Id String id;

    private final String name;

    private final LocalDate date;

    private final String city;

    @GeoIndexed(geoJson = true)
    private final Point location;

    @To(lazy = true)
    @EqualsAndHashCode.Exclude
    private List<Score> standings;

    @Relations(
            edges = Score.class,
            direction = Relations.Direction.INBOUND,
            lazy = true
    )
    @EqualsAndHashCode.Exclude
    private List<Player> players;
}

