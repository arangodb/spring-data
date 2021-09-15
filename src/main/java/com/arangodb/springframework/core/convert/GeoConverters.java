package com.arangodb.springframework.core.convert;

import com.arangodb.springframework.core.geo.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.geo.*;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;


public class GeoConverters {

    private static final String TYPE = "type";
    private static final String COORDS = "coordinates";

    private static final Map<String, Function<DBDocumentEntity, GeoJson<?>>> toGeoJsonConverters = new HashMap<>();

    static {
        toGeoJsonConverters.put("Point", DBDocumentEntityToGeoJsonPointConverter.INSTANCE::convert);
        toGeoJsonConverters.put("MultiPoint", DBDocumentEntityToGeoJsonMultiPointConverter.INSTANCE::convert);
        toGeoJsonConverters.put("LineString", DBDocumentEntityToGeoJsonLineStringConverter.INSTANCE::convert);
        toGeoJsonConverters.put("MultiLineString", DBDocumentEntityToGeoJsonMultiLineStringConverter.INSTANCE::convert);
        toGeoJsonConverters.put("Polygon", DBDocumentEntityToGeoJsonPolygonConverter.INSTANCE::convert);
    }

    public static Collection<Converter<?, ?>> getConvertersToRegister() {
        return Arrays.asList(
                // writing converters
                PointToDBDocumentEntityConverter.INSTANCE,
                GeoJsonPointToDBDocumentEntityConverter.INSTANCE,
                GeoJsonMultiPointToDBDocumentEntityConverter.INSTANCE,
                GeoJsonLineStringToDBDocumentEntityConverter.INSTANCE,
                GeoJsonMultiLineStringToDBDocumentEntityConverter.INSTANCE,
                PolygonToDBDocumentEntityConverter.INSTANCE,
                GeoJsonPolygonToDBDocumentEntityConverter.INSTANCE,
                BoxToDBDocumentEntityConverter.INSTANCE,

                // reading converters
                DBDocumentEntityToGeoJsonConverter.INSTANCE,
                DBDocumentEntityToPointConverter.INSTANCE,
                DBDocumentEntityToGeoJsonPointConverter.INSTANCE,
                DBDocumentEntityToGeoJsonMultiPointConverter.INSTANCE,
                DBDocumentEntityToGeoJsonLineStringConverter.INSTANCE,
                DBDocumentEntityToGeoJsonMultiLineStringConverter.INSTANCE,
                DBDocumentEntityToPolygonConverter.INSTANCE,
                DBDocumentEntityToGeoJsonPolygonConverter.INSTANCE
        );
    }

    private GeoConverters() {
    }

    @WritingConverter
    enum PointToDBDocumentEntityConverter implements Converter<Point, DBDocumentEntity> {

        INSTANCE;

        @Override
        public DBDocumentEntity convert(Point source) {
            return GeoJsonPointToDBDocumentEntityConverter.INSTANCE.convert(new GeoJsonPoint(source));
        }
    }

    @WritingConverter
    enum GeoJsonPointToDBDocumentEntityConverter implements Converter<GeoJsonPoint, DBDocumentEntity> {

        INSTANCE;

        @Override
        public DBDocumentEntity convert(GeoJsonPoint source) {
            DBDocumentEntity d = new DBDocumentEntity();
            d.put(TYPE, source.getType());
            d.put(COORDS, source.getCoordinates());
            return d;
        }
    }

    @WritingConverter
    enum GeoJsonMultiPointToDBDocumentEntityConverter implements Converter<GeoJsonMultiPoint, DBDocumentEntity> {

        INSTANCE;

        @Override
        public DBDocumentEntity convert(GeoJsonMultiPoint source) {
            DBDocumentEntity d = new DBDocumentEntity();
            d.put(TYPE, source.getType());
            d.put(COORDS, pointsToList(source.getCoordinates()));
            return d;
        }
    }

    @WritingConverter
    enum GeoJsonLineStringToDBDocumentEntityConverter implements Converter<GeoJsonLineString, DBDocumentEntity> {

        INSTANCE;

        @Override
        public DBDocumentEntity convert(GeoJsonLineString source) {
            DBDocumentEntity d = new DBDocumentEntity();
            d.put(TYPE, source.getType());
            d.put(COORDS, pointsToList(source.getCoordinates()));
            return d;
        }
    }

    @WritingConverter
    enum GeoJsonMultiLineStringToDBDocumentEntityConverter implements Converter<GeoJsonMultiLineString, DBDocumentEntity> {

        INSTANCE;

        @Override
        public DBDocumentEntity convert(GeoJsonMultiLineString source) {
            DBDocumentEntity d = new DBDocumentEntity();
            d.put(TYPE, source.getType());
            d.put(COORDS, lineStringsToList(source.getCoordinates()));
            return d;
        }
    }

    @WritingConverter
    enum PolygonToDBDocumentEntityConverter implements Converter<Polygon, DBDocumentEntity> {

        INSTANCE;

        @Override
        public DBDocumentEntity convert(Polygon source) {
            return GeoJsonPolygonToDBDocumentEntityConverter.INSTANCE.convert(new GeoJsonPolygon(source.getPoints()));
        }
    }

    @WritingConverter
    enum GeoJsonPolygonToDBDocumentEntityConverter implements Converter<GeoJsonPolygon, DBDocumentEntity> {

        INSTANCE;

        @Override
        public DBDocumentEntity convert(GeoJsonPolygon source) {
            DBDocumentEntity d = new DBDocumentEntity();
            d.put(TYPE, source.getType());
            d.put(COORDS, lineStringsToList(source.getCoordinates()));
            return d;
        }
    }

    @WritingConverter
    enum BoxToDBDocumentEntityConverter implements Converter<Box, DBDocumentEntity> {

        INSTANCE;

        @Override
        public DBDocumentEntity convert(Box source) {
            Point a = source.getFirst();
            Point b = source.getSecond();
            Polygon p = new Polygon(
                    new Point(a.getX(), a.getY()),
                    new Point(a.getX(), b.getY()),
                    new Point(b.getX(), b.getY()),
                    new Point(b.getX(), a.getY()),
                    new Point(a.getX(), a.getY())
            );
            return PolygonToDBDocumentEntityConverter.INSTANCE.convert(p);
        }
    }

    @ReadingConverter
    enum DBDocumentEntityToGeoJsonConverter implements Converter<DBDocumentEntity, GeoJson<?>> {

        INSTANCE;

        @Override
        public GeoJson<?> convert(DBDocumentEntity source) {
            String type = (String) source.get(TYPE);
            return toGeoJsonConverters.get(type).apply(source);
        }
    }

    @ReadingConverter
    enum DBDocumentEntityToPointConverter implements Converter<DBDocumentEntity, Point> {

        INSTANCE;

        @Override
        public Point convert(DBDocumentEntity source) {
            return DBDocumentEntityToGeoJsonPointConverter.INSTANCE.convert(source);
        }
    }

    @ReadingConverter
    enum DBDocumentEntityToGeoJsonPointConverter implements Converter<DBDocumentEntity, GeoJsonPoint> {

        INSTANCE;

        @Override
        @SuppressWarnings("unchecked")
        public GeoJsonPoint convert(DBDocumentEntity source) {
            Assert.isTrue("Point".equals(source.get(TYPE)), "source type must be 'Point'");
            return toPoint((List<Number>) source.get(COORDS));
        }
    }

    @ReadingConverter
    enum DBDocumentEntityToGeoJsonMultiPointConverter implements Converter<DBDocumentEntity, GeoJsonMultiPoint> {

        INSTANCE;

        @Override
        @SuppressWarnings("unchecked")
        public GeoJsonMultiPoint convert(DBDocumentEntity source) {
            Assert.isTrue("MultiPoint".equals(source.get(TYPE)), "source type must be 'MultiPoint'");
            Iterable<Iterable<Number>> coords = (Iterable<Iterable<Number>>) source.get(COORDS);
            return new GeoJsonMultiPoint(toListOfPoints(coords));
        }
    }

    @ReadingConverter
    enum DBDocumentEntityToGeoJsonLineStringConverter implements Converter<DBDocumentEntity, GeoJsonLineString> {

        INSTANCE;

        @Override
        @SuppressWarnings("unchecked")
        public GeoJsonLineString convert(DBDocumentEntity source) {
            Assert.isTrue("LineString".equals(source.get(TYPE)), "source type must be 'LineString'");
            Iterable<Iterable<Number>> coords = (Iterable<Iterable<Number>>) source.get(COORDS);
            return new GeoJsonLineString(toListOfPoints(coords));
        }
    }

    @ReadingConverter
    enum DBDocumentEntityToGeoJsonMultiLineStringConverter implements Converter<DBDocumentEntity, GeoJsonMultiLineString> {

        INSTANCE;

        @Override
        @SuppressWarnings("unchecked")
        public GeoJsonMultiLineString convert(DBDocumentEntity source) {
            Assert.isTrue("MultiLineString".equals(source.get(TYPE)), "source type must be 'MultiLineString'");
            Iterable<Iterable<Iterable<Number>>> coords = (Iterable<Iterable<Iterable<Number>>>) source.get(COORDS);
            return new GeoJsonMultiLineString(toListOfLineStrings(coords));
        }
    }

    @ReadingConverter
    enum DBDocumentEntityToPolygonConverter implements Converter<DBDocumentEntity, Polygon> {

        INSTANCE;

        @Override
        public Polygon convert(DBDocumentEntity source) {
            return DBDocumentEntityToGeoJsonPolygonConverter.INSTANCE.convert(source);
        }
    }

    @ReadingConverter
    enum DBDocumentEntityToGeoJsonPolygonConverter implements Converter<DBDocumentEntity, GeoJsonPolygon> {

        INSTANCE;

        @Override
        @SuppressWarnings("unchecked")
        public GeoJsonPolygon convert(DBDocumentEntity source) {
            Assert.isTrue("Polygon".equals(source.get(TYPE)), "source type must be 'Polygon'");
            Iterable<Iterable<Iterable<Number>>> coords = (Iterable<Iterable<Iterable<Number>>>) source.get(COORDS);
            Iterator<Iterable<Iterable<Number>>> it = coords.iterator();
            GeoJsonPolygon p = new GeoJsonPolygon(toListOfPoints(it.next()));
            while (it.hasNext()){
                p = p.withInnerRing(toListOfPoints(it.next()));
            }
            return p;
        }
    }


    private static List<Double> pointToList(Point point) {
        return Arrays.asList(point.getX(), point.getY());
    }

    private static List<List<Double>> pointsToList(Iterable<Point> points) {
        List<List<Double>> l = new ArrayList<>();
        for (Point p : points) {
            l.add(pointToList(p));
        }
        return l;
    }

    private static List<List<List<Double>>> lineStringsToList(Iterable<GeoJsonLineString> lineStrings) {
        List<List<List<Double>>> l = new ArrayList<>();
        for (GeoJsonLineString ls : lineStrings) {
            l.add(pointsToList(ls.getCoordinates()));
        }
        return l;
    }

    private static GeoJsonPoint toPoint(Iterable<Number> coords) {
        Iterator<Number> it = coords.iterator();
        double x = it.next().doubleValue();
        double y = it.next().doubleValue();
        return new GeoJsonPoint(x, y);
    }

    private static List<Point> toListOfPoints(Iterable<Iterable<Number>> coords) {
        List<Point> points = new ArrayList<>();
        for (Iterable<Number> point : coords) {
            points.add(toPoint(point));
        }
        return points;
    }

    private static List<GeoJsonLineString> toListOfLineStrings(Iterable<Iterable<Iterable<Number>>> coords) {
        List<GeoJsonLineString> lineStrings = new ArrayList<>();
        for (Iterable<Iterable<Number>> c : coords) {
            lineStrings.add(new GeoJsonLineString(toListOfPoints(c)));
        }
        return lineStrings;
    }

}
