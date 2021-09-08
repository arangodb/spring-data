package com.arangodb.springframework.core.convert;

import com.arangodb.springframework.core.geo.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.geo.*;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;


// TODO:
// - test @GeoIndex and @GeoIndexed on GeoJson fields (geoJson=true)
// - query derivation with geo and geoJson types

public class GeoConverters {

    private static final Map<String, Function<DBDocumentEntity, GeoJson<?>>> toGeoJsonConverters = new HashMap<>();

    static {
        toGeoJsonConverters.put("Point", DBDocumentEntityToGeoJsonPointConverter.INSTANCE::convert);
        toGeoJsonConverters.put("MultiPoint", DBDocumentEntityToGeoJsonMultiPointConverter.INSTANCE::convert);
        toGeoJsonConverters.put("LineString", DBDocumentEntityToGeoJsonLineStringConverter.INSTANCE::convert);
        toGeoJsonConverters.put("MultiLineString", DBDocumentEntityToGeoJsonMultiLineStringConverter.INSTANCE::convert);
//        toGeoJsonConverters.put("Polygon", DBDocumentEntityToGeoJsonPolygonConverter.INSTANCE::convert);
    }

    public static Collection<Converter<?, ?>> getConvertersToRegister() {
        return Arrays.asList(
                // writing converters
                PointToDBDocumentEntityConverter.INSTANCE,
                GeoJsonPointToDBDocumentEntityConverter.INSTANCE,
                GeoJsonMultiPointToDBDocumentEntityConverter.INSTANCE,
                GeoJsonLineStringToDBDocumentEntityConverter.INSTANCE,
                GeoJsonMultiLineStringToDBDocumentEntityConverter.INSTANCE,

                // reading converters
                DBDocumentEntityToGeoJsonConverter.INSTANCE,
                DBDocumentEntityToPointConverter.INSTANCE,
                DBDocumentEntityToGeoJsonPointConverter.INSTANCE,
                DBDocumentEntityToGeoJsonMultiPointConverter.INSTANCE,
                DBDocumentEntityToGeoJsonLineStringConverter.INSTANCE,
                DBDocumentEntityToGeoJsonMultiLineStringConverter.INSTANCE
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
            d.put("type", source.getType());
            d.put("coordinates", source.getCoordinates());
            return d;
        }
    }

    @WritingConverter
    enum GeoJsonMultiPointToDBDocumentEntityConverter implements Converter<GeoJsonMultiPoint, DBDocumentEntity> {

        INSTANCE;

        @Override
        public DBDocumentEntity convert(GeoJsonMultiPoint source) {
            DBDocumentEntity d = new DBDocumentEntity();
            d.put("type", source.getType());
            d.put("coordinates", pointsToList(source.getCoordinates()));
            return d;
        }
    }

    @WritingConverter
    enum GeoJsonLineStringToDBDocumentEntityConverter implements Converter<GeoJsonLineString, DBDocumentEntity> {

        INSTANCE;

        @Override
        public DBDocumentEntity convert(GeoJsonLineString source) {
            DBDocumentEntity d = new DBDocumentEntity();
            d.put("type", source.getType());
            d.put("coordinates", pointsToList(source.getCoordinates()));
            return d;
        }
    }

    @WritingConverter
    enum GeoJsonMultiLineStringToDBDocumentEntityConverter implements Converter<GeoJsonMultiLineString, DBDocumentEntity> {

        INSTANCE;

        @Override
        public DBDocumentEntity convert(GeoJsonMultiLineString source) {
            DBDocumentEntity d = new DBDocumentEntity();
            d.put("type", source.getType());
            d.put("coordinates", lineStringsToList(source.getCoordinates()));
            return d;
        }
    }

    @ReadingConverter
    enum DBDocumentEntityToGeoJsonConverter implements Converter<DBDocumentEntity, GeoJson<?>> {

        INSTANCE;

        @Override
        public GeoJson<?> convert(DBDocumentEntity source) {
            String type = (String) source.get("type");
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
            Assert.isTrue("Point".equals(source.get("type")), "source type must be 'Point'");
            return toPoint((List<Number>) source.get("coordinates"));
        }
    }

    @ReadingConverter
    enum DBDocumentEntityToGeoJsonMultiPointConverter implements Converter<DBDocumentEntity, GeoJsonMultiPoint> {

        INSTANCE;

        @Override
        @SuppressWarnings("unchecked")
        public GeoJsonMultiPoint convert(DBDocumentEntity source) {
            Assert.isTrue("MultiPoint".equals(source.get("type")), "source type must be 'MultiPoint'");
            Iterable<Iterable<Number>> coords = (Iterable<Iterable<Number>>) source.get("coordinates");
            return new GeoJsonMultiPoint(toListOfPoint(coords));
        }
    }

    @ReadingConverter
    enum DBDocumentEntityToGeoJsonLineStringConverter implements Converter<DBDocumentEntity, GeoJsonLineString> {

        INSTANCE;

        @Override
        @SuppressWarnings("unchecked")
        public GeoJsonLineString convert(DBDocumentEntity source) {
            Assert.isTrue("LineString".equals(source.get("type")), "source type must be 'LineString'");
            Iterable<Iterable<Number>> coords = (Iterable<Iterable<Number>>) source.get("coordinates");
            return new GeoJsonLineString(toListOfPoint(coords));
        }
    }

    @ReadingConverter
    enum DBDocumentEntityToGeoJsonMultiLineStringConverter implements Converter<DBDocumentEntity, GeoJsonMultiLineString> {

        INSTANCE;

        @Override
        @SuppressWarnings("unchecked")
        public GeoJsonMultiLineString convert(DBDocumentEntity source) {
            Assert.isTrue("MultiLineString".equals(source.get("type")), "source type must be 'MultiLineString'");
            Iterable<Iterable<Iterable<Number>>> coords = (Iterable<Iterable<Iterable<Number>>>) source.get("coordinates");
            return new GeoJsonMultiLineString(toListOfLineStrings(coords));
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

    private static List<Point> toListOfPoint(Iterable<Iterable<Number>> coords) {
        List<Point> points = new ArrayList<>();
        for (Iterable<Number> point : coords) {
            points.add(toPoint(point));
        }
        return points;
    }

    private static List<GeoJsonLineString> toListOfLineStrings(Iterable<Iterable<Iterable<Number>>> coords) {
        List<GeoJsonLineString> lineStrings = new ArrayList<>();
        for (Iterable<Iterable<Number>> c : coords) {
            lineStrings.add(new GeoJsonLineString(toListOfPoint(c)));
        }
        return lineStrings;
    }

}
