package com.arangodb.springframework.component;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component("filterGenerator")
public class FilterGenerator {

    public String allEqual(String col, Map<String, Object> kv) {
        return kv.entrySet().stream()
                .map(it -> col + "." + it.getKey() + " == " + escape(it.getValue()))
                .collect(Collectors.joining(" AND "));
    }

    private Object escape(Object o) {
        if (o instanceof String) return "\"" + o + "\"";
        else return o;
    }

}
