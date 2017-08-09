package com.arangodb.springframework.core.repository;

import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
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
        String delimiter = example.getMatcher().isAllMatching() ? " AND " : " OR ";
        StringBuilder predicateBuilder = new StringBuilder();
        ArangoPersistentEntity<?> persistentEntity = context.getPersistentEntity(example.getProbeType());
        PersistentPropertyAccessor accessor = persistentEntity.getPropertyAccessor(example.getProbe());
        persistentEntity.doWithProperties((ArangoPersistentProperty property) -> {
            Object value = accessor.getProperty(property);
            if (!example.getMatcher().isIgnoredPath(property.getName()) && value != null || example.getMatcher().getNullHandler().equals(ExampleMatcher.NullHandler.INCLUDE)) {
                String fieldName = property.getFieldName();
                if (predicateBuilder.length() > 0) predicateBuilder.append(delimiter);
                String binding = Integer.toString(bindVars.size());
                String clause = "true";
                if (String.class.isAssignableFrom(value.getClass())) {
                    ExampleMatcher.PropertySpecifier specifier = example.getMatcher().getPropertySpecifiers().getForPath(property.getName());
                    boolean ignoreCase = specifier == null ? example.getMatcher().isIgnoreCaseEnabled() : specifier.getIgnoreCase();
                    ExampleMatcher.StringMatcher stringMatcher
                            = (specifier == null || specifier.getStringMatcher() == ExampleMatcher.StringMatcher.DEFAULT)
                            ? example.getMatcher().getDefaultStringMatcher() : specifier.getStringMatcher();
                    String string = (String) value;
                    clause = String.format("REGEX_TEST(e.%s, @%s, %b)", fieldName, binding, ignoreCase);
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
                    clause = "e." + fieldName + " == @" + binding;
                }
                predicateBuilder.append(clause);
                bindVars.put(binding, value);
            }
        });
        return predicateBuilder.toString();
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
