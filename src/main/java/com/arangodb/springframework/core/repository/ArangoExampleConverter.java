package com.arangodb.springframework.core.repository;

import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Created by user on 08/08/17.
 */
public class ArangoExampleConverter<T> {

    private final ArangoMappingContext context;

    public ArangoExampleConverter(ArangoMappingContext context) {
        this.context = context;
    }

    public String convertExampleToPredicate(Example<T> example, final Map<String, Object> bindVars) {
        StringBuilder predicateBuilder = new StringBuilder();
        ArangoPersistentEntity<?> persistentEntity = context.getPersistentEntity(example.getProbeType());
        Assert.isTrue(example.getProbe() != null, "Probe in Example cannot be null");
        traversePropertyTree(example, predicateBuilder, bindVars, "", "", persistentEntity, example.getProbe());
        return predicateBuilder.toString();
    }

    private void traversePropertyTree(Example<T> example, StringBuilder predicateBuilder, Map<String, Object> bindVars,
                                      String path, String javaPath, ArangoPersistentEntity<?> entity, Object object) {
        PersistentPropertyAccessor accessor = entity.getPropertyAccessor(object);
        entity.doWithProperties((ArangoPersistentProperty property) -> {
            String fullPath = path + (path.length() == 0 ? "" : ".") + property.getFieldName();
            String fullJavaPath = javaPath + (javaPath.length() == 0 ? "" : ".") + property.getName();
            Object value = accessor.getProperty(property);
            if (property.isEntity() && value != null) {
                ArangoPersistentEntity<?> persistentEntity = context.getPersistentEntity(property.getType());
                traversePropertyTree(example, predicateBuilder, bindVars, fullPath, fullJavaPath, persistentEntity, value);
            } else if (!example.getMatcher().isIgnoredPath(fullJavaPath) && (value != null
                    || example.getMatcher().getNullHandler().equals(ExampleMatcher.NullHandler.INCLUDE))) {
                addPredicate(example, predicateBuilder, bindVars, fullPath, fullJavaPath, value);
            }
        });
    }

    private void addPredicate(Example<T> example, StringBuilder predicateBuilder, Map<String, Object> bindVars,
                              String fullPath, String fullJavaPath, Object value) {
        String delimiter = example.getMatcher().isAllMatching() ? " AND " : " OR ";
        if (predicateBuilder.length() > 0) { predicateBuilder.append(delimiter); }
        String binding = Integer.toString(bindVars.size());
        String clause;
        ExampleMatcher.PropertySpecifier specifier = example.getMatcher().getPropertySpecifiers().getForPath(fullPath);
        if (specifier != null && value != null) { value = specifier.transformValue(value); }
        if (value == null) {
            clause = String.format("e.%s == null", fullPath);
        } else if (String.class.isAssignableFrom(value.getClass())) {
            boolean ignoreCase = specifier == null ? example.getMatcher().isIgnoreCaseEnabled()
                    : (specifier.getIgnoreCase() == null ? false : specifier.getIgnoreCase());
            ExampleMatcher.StringMatcher stringMatcher = (specifier == null || specifier.getStringMatcher()
                    == ExampleMatcher.StringMatcher.DEFAULT) ? example.getMatcher().getDefaultStringMatcher()
                    : specifier.getStringMatcher();
            String string = (String) value;
            clause = String.format("REGEX_TEST(e.%s, @%s, %b)", fullPath, binding, ignoreCase);
            switch (stringMatcher) {
                case DEFAULT:
                case EXACT:
                    value = "^" + escape(string) + "$";
                    break;
                case STARTING:
                    value = "^" + escape(string);
                    break;
                case ENDING:
                    value = escape(string) + "$";
                    break;
            }
        } else {
            clause = "e." + fullPath + " == @" + binding;
        }
        predicateBuilder.append(clause);
        if (value != null) { bindVars.put(binding, value); }
    }

    private static final Set<Character> SPECIAL_CHARACTERS = new HashSet<>();

    static {
        SPECIAL_CHARACTERS.add('\\');
        SPECIAL_CHARACTERS.add('.');
        SPECIAL_CHARACTERS.add('?');
        SPECIAL_CHARACTERS.add('[');
        SPECIAL_CHARACTERS.add(']');
        SPECIAL_CHARACTERS.add('*');
        SPECIAL_CHARACTERS.add('{');
        SPECIAL_CHARACTERS.add('}');
        SPECIAL_CHARACTERS.add('(');
        SPECIAL_CHARACTERS.add(')');
        SPECIAL_CHARACTERS.add('^');
        SPECIAL_CHARACTERS.add('$');
    }

    private static String escape(String string) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Character character : string.toCharArray()) {
            if (SPECIAL_CHARACTERS.contains(character)) stringBuilder.append('\\');
            stringBuilder.append(character);
        }
        return stringBuilder.toString();
    }
}
