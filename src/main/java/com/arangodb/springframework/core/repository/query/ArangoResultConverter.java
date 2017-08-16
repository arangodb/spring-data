package com.arangodb.springframework.core.repository.query;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.convert.DBDocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Slice;
import org.springframework.data.geo.*;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by markfmccormick on 07/08/2017.
 */
public class ArangoResultConverter {

    private ArangoParameterAccessor accessor;
    private ArangoCursor result;
    private ArangoOperations operations;
    private Class domainClass;

    private static Map<Object, Method> TYPE_MAP = new HashMap<>();

    static {
        try {
            TYPE_MAP.put(List.class, ArangoResultConverter.class.getMethod("convertList"));
            TYPE_MAP.put(Iterable.class, ArangoResultConverter.class.getMethod("convertList"));
            TYPE_MAP.put(Collection.class, ArangoResultConverter.class.getMethod("convertList"));
            TYPE_MAP.put(Page.class, ArangoResultConverter.class.getMethod("convertPage"));
            TYPE_MAP.put(Slice.class, ArangoResultConverter.class.getMethod("convertPage"));
            TYPE_MAP.put(Set.class, ArangoResultConverter.class.getMethod("convertSet"));
            TYPE_MAP.put(BaseDocument.class, ArangoResultConverter.class.getMethod("convertBaseDocument"));
            TYPE_MAP.put(BaseEdgeDocument.class, ArangoResultConverter.class.getMethod("convertBaseEdgeDocument"));
            TYPE_MAP.put(ArangoCursor.class, ArangoResultConverter.class.getMethod("convertArangoCursor"));
            TYPE_MAP.put(GeoResult.class, ArangoResultConverter.class.getMethod("convertGeoResult"));
            TYPE_MAP.put(GeoResults.class, ArangoResultConverter.class.getMethod("convertGeoResults"));
            TYPE_MAP.put(GeoPage.class, ArangoResultConverter.class.getMethod("convertGeoPage"));
            TYPE_MAP.put(Optional.class, ArangoResultConverter.class.getMethod("convertOptional"));
            TYPE_MAP.put("array", ArangoResultConverter.class.getMethod("convertArray"));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public ArangoResultConverter(ArangoParameterAccessor accessor, ArangoCursor result, ArangoOperations operations, Class domainClass) {
        this.accessor = accessor;
        this.result = result;
        this.operations = operations;
        this.domainClass = domainClass;
    }

    public Object convertResult(Class type) {
        try {
            if (type.isArray()) { return TYPE_MAP.get("array").invoke(this, null); }
            if (!TYPE_MAP.containsKey(type)) { return getNext(result); }
            return TYPE_MAP.get(type).invoke(this, null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Set buildSet(ArangoCursor cursor) {
        Set set = new HashSet();
        cursor.forEachRemaining(set::add);
        return set;
    }

    private GeoResult buildGeoResult(ArangoCursor cursor) {
        GeoResult geoResult = null;
        while (cursor.hasNext() && geoResult == null) {
            Object object = cursor.next();
            Map<String, Object> map = (Map<String, Object>) object;
            Double distanceInMeters = (Double) map.get("_distance");
            if (distanceInMeters == null) { continue; }
            Object entity = operations.getConverter().read(domainClass, new DBDocumentEntity(map));
            Distance distance = new Distance(distanceInMeters / 1000, Metrics.KILOMETERS);
            geoResult = new GeoResult(entity, distance);
        }
        return geoResult;
    }

    private GeoResult buildGeoResult(Object object) {
        if (object == null) { return null; }
        Map<String, Object> map = (Map<String, Object>) object;
        Object entity = operations.getConverter().read(domainClass, new DBDocumentEntity(map));
        Double distanceInMeters = (Double) map.get("_distance");
        if (distanceInMeters == null) { return null; }
        Distance distance = new Distance(distanceInMeters / 1000, Metrics.KILOMETERS);
        return new GeoResult(entity, distance);
    }

    private GeoResults buildGeoResults(ArangoCursor cursor) {
        List<GeoResult> list = new LinkedList<>();
        cursor.forEachRemaining(o -> {
            GeoResult geoResult = buildGeoResult(o);
            if (geoResult != null) { list.add(geoResult); }
        });
        return new GeoResults(list);
    }

    public Optional convertOptional() { return Optional.ofNullable(getNext(result)); }

    public List convertList() {
        return result.asListRemaining();
    }

    public PageImpl convertPage() {
        return new PageImpl(result.asListRemaining(), accessor.getPageable(), result.getStats().getFullCount());
    }
    public Set convertSet() {
        return buildSet(result);
    }

    public BaseDocument convertBaseDocument() {
        Object next = getNext(result);
        return next == null ? null : new BaseDocument((Map<String, Object>) next);
    }

    public BaseEdgeDocument convertBaseEdgeDocument() {
        Object next = getNext(result);
        return next == null ? null : new BaseEdgeDocument((Map<String, Object>) next);
    }

    public ArangoCursor convertArangoCursor() {
        return result;
    }

    public GeoResult convertGeoResult() { return buildGeoResult(result); }

    public GeoResults convertGeoResults() {
        return buildGeoResults(result);
    }

    public GeoPage convertGeoPage() {
        return new GeoPage(buildGeoResults(result), accessor.getPageable(), result.getStats().getFullCount());
    }

    public Object convertArray() {
        return result.asListRemaining().toArray();
    }

    private Object getNext(ArangoCursor cursor) { return cursor.hasNext() ? cursor.next() : null; }
}


