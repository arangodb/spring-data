package com.arangodb.springframework.core.repository.query.derived.geo;

import org.springframework.data.domain.Range;
import org.springframework.data.geo.Point;

public class Ring<T extends Comparable<T>> {

    private Point point;
    private Range<T> range;

    public Ring(Point point, Range<T> range) {
        this.point = point;
        this.range = range;
    }

    public Point getPoint() { return point; }

    public Range<T> getRange() { return range; }
}
