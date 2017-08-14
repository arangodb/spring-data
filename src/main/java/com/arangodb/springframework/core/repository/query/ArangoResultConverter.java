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

    private static Map<Object, Method> typeMap = new HashMap<>();

    static {
        try {
            typeMap.put(List.class, ArangoResultConverter.class.getMethod("convertList"));
            typeMap.put(Iterable.class, ArangoResultConverter.class.getMethod("convertList"));
            typeMap.put(Collection.class, ArangoResultConverter.class.getMethod("convertList"));
            typeMap.put(Page.class, ArangoResultConverter.class.getMethod("convertPage"));
            typeMap.put(Slice.class, ArangoResultConverter.class.getMethod("convertPage"));
            typeMap.put(Set.class, ArangoResultConverter.class.getMethod("convertSet"));
            typeMap.put(BaseDocument.class, ArangoResultConverter.class.getMethod("convertBaseDocument"));
            typeMap.put(BaseEdgeDocument.class, ArangoResultConverter.class.getMethod("convertBaseEdgeDocument"));
            typeMap.put(ArangoCursor.class, ArangoResultConverter.class.getMethod("convertArangoCursor"));
            typeMap.put(GeoResult.class, ArangoResultConverter.class.getMethod("convertGeoResult"));
            typeMap.put(GeoResults.class, ArangoResultConverter.class.getMethod("convertGeoResults"));
            typeMap.put(GeoPage.class, ArangoResultConverter.class.getMethod("convertGeoPage"));
            typeMap.put(Optional.class, ArangoResultConverter.class.getMethod("convertOptional"));
            typeMap.put("array", ArangoResultConverter.class.getMethod("convertArray"));
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
            if (type.isArray()) { return typeMap.get("array").invoke(this, null); }
            if (!typeMap.containsKey(type)) { return result.next(); }
            return typeMap.get(type).invoke(this, null);
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

    private GeoResult buildGeoResult(Object object) {
        Map<String, Object> map = (Map<String, Object>) object;
        Object entity = operations.getConverter().read(domainClass, new DBDocumentEntity(map));
        Distance distance = new Distance(((double) map.get("_distance")) / 1000, Metrics.KILOMETERS);
        return new GeoResult(entity, distance);
    }

    private GeoResults buildGeoResults(ArangoCursor cursor) {
        List<GeoResult> list = new LinkedList<>();
        cursor.forEachRemaining(o -> list.add(buildGeoResult(o)));
        return new GeoResults(list);
    }

    public Optional convertOptional() { return Optional.ofNullable(result.hasNext() ? result.next() : null); }

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
        return new BaseDocument((Map<String, Object>) result.next());
    }

    public BaseEdgeDocument convertBaseEdgeDocument() {
        return new BaseEdgeDocument((Map<String, Object>) result.next());
    }

    public ArangoCursor convertArangoCursor() {
        return result;
    }

    public GeoResult convertGeoResult() {
        return buildGeoResult(result.next());
    }

    public GeoResults convertGeoResults() {
        return buildGeoResults(result);
    }

    public GeoPage convertGeoPage() {
        return new GeoPage(buildGeoResults(result), accessor.getPageable(), result.getStats().getFullCount());
    }

    public Object convertArray() {
        return result.asListRemaining().toArray();
    }

}


