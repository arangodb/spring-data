package com.arangodb.springframework.core.convert;

import com.arangodb.springframework.core.geo.GeoJson;
import com.arangodb.springframework.core.geo.GeoJsonPoint;
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
//        toGeoJsonConverters.put("MultiPoint", DBDocumentEntityToGeoJsonMultiPointConverter.INSTANCE::convert);
//        toGeoJsonConverters.put("LineString", DBDocumentEntityToGeoJsonLineStringConverter.INSTANCE::convert);
//        toGeoJsonConverters.put("MultiLineString", DBDocumentEntityToGeoJsonMultiLineStringConverter.INSTANCE::convert);
//        toGeoJsonConverters.put("Polygon", DBDocumentEntityToGeoJsonPolygonConverter.INSTANCE::convert);
    }

    public static Collection<Converter<?, ?>> getConvertersToRegister() {
        return Arrays.asList(
                PointToDBDocumentEntityConverter.INSTANCE,
                GeoJsonToDBDocumentEntityConverter.INSTANCE,
                DBDocumentEntityToGeoJsonConverter.INSTANCE,
                DBDocumentEntityToPointConverter.INSTANCE,
                DBDocumentEntityToGeoJsonPointConverter.INSTANCE
        );
    }

    private GeoConverters() {
    }

    @WritingConverter
    enum PointToDBDocumentEntityConverter implements Converter<Point, DBDocumentEntity> {

        INSTANCE;

        @Override
        public DBDocumentEntity convert(Point source) {
            return GeoJsonToDBDocumentEntityConverter.INSTANCE.convert(new GeoJsonPoint(source));
        }
    }

    @WritingConverter
    enum GeoJsonToDBDocumentEntityConverter implements Converter<GeoJson<?>, DBDocumentEntity> {

        INSTANCE;

        @Override
        public DBDocumentEntity convert(GeoJson<?> source) {
            DBDocumentEntity d = new DBDocumentEntity();
            d.put("type", source.getType());
            d.put("coordinates", source.getCoordinates());
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
            List<Number> coords = (List<Number>) source.get("coordinates");
            return new GeoJsonPoint(coords.get(0).doubleValue(), coords.get(1).doubleValue());
        }
    }


}
