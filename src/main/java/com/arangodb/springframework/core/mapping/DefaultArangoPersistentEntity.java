/*
 * DISCLAIMER
 *
 * Copyright 2017 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.springframework.core.mapping;

import com.arangodb.entity.CollectionType;
import com.arangodb.entity.ReplicationFactor;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.springframework.annotation.*;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.TargetAwareIdentifierAccessor;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @param <T>
 * @author Mark Vollmary
 */
public class DefaultArangoPersistentEntity<T> extends BasicPersistentEntity<T, ArangoPersistentProperty>
        implements ArangoPersistentEntity<T> {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private final String collection;
    private final Expression expression;
    private final StandardEvaluationContext context;

    private ArangoPersistentProperty arangoIdProperty;
    private ArangoPersistentProperty revProperty;
    private ArangoPersistentProperty ttlIndexedProperty;
    private final Document documentAnnotation;
    private final Edge edgeAnnotation;
    private final Map<String, ArangoPersistentProperty> computedValueProperties;
    private final Collection<ArangoPersistentProperty> persistentIndexedProperties;
    private final Collection<ArangoPersistentProperty> geoIndexedProperties;
    private final Collection<ArangoPersistentProperty> fulltextIndexedProperties;

    private final Map<Class<? extends Annotation>, Set<? extends Annotation>> repeatableAnnotationCache;

    public DefaultArangoPersistentEntity(final TypeInformation<T> information) {
        super(information);
        context = new StandardEvaluationContext();
        computedValueProperties = new HashMap<>();
        persistentIndexedProperties = new ArrayList<>();
        geoIndexedProperties = new ArrayList<>();
        fulltextIndexedProperties = new ArrayList<>();
        repeatableAnnotationCache = new HashMap<>();
        documentAnnotation = findAnnotation(Document.class);
        edgeAnnotation = findAnnotation(Edge.class);
        String col = StringUtils.uncapitalize(information.getType().getSimpleName());
        if (edgeAnnotation != null) {
            collection = StringUtils.hasText(edgeAnnotation.value()) ? edgeAnnotation.value() : col;
        } else if (documentAnnotation != null) {
            collection = StringUtils.hasText(documentAnnotation.value()) ? documentAnnotation.value() : col;
        } else {
            collection = col;
        }
        expression = PARSER.parseExpression(collection, ParserContext.TEMPLATE_EXPRESSION);
    }

    private static CollectionCreateOptions createDocumentCollectionOptions(final Document annotation) {
        final CollectionCreateOptions options = new CollectionCreateOptions().type(CollectionType.DOCUMENT);
        options.waitForSync(annotation.waitForSync()).isSystem(annotation.isSystem());
        if (annotation.replicationFactor() > -1) {
            options.replicationFactor(annotation.replicationFactor());
        }
        if (annotation.satellite()) {
            options.replicationFactor(ReplicationFactor.ofSatellite());
        }
        final String[] shardKeys = annotation.shardKeys();
        if (shardKeys.length > 0) {
            options.shardKeys(shardKeys);
        }
        if (annotation.numberOfShards() > -1) {
            options.numberOfShards(annotation.numberOfShards());
        }
        if (annotation.allowUserKeys()) {
            options.keyOptions(true, annotation.keyType(),
                    annotation.keyIncrement() > -1 ? annotation.keyIncrement() : null,
                    annotation.keyOffset() > -1 ? annotation.keyOffset() : null);
        }
        return options;
    }

    private static CollectionCreateOptions createEdgeCollectionOptions(final Edge annotation) {
        final CollectionCreateOptions options = new CollectionCreateOptions().type(CollectionType.EDGES);
        options.waitForSync(annotation.waitForSync()).isSystem(annotation.isSystem());
        if (annotation.replicationFactor() > -1) {
            options.replicationFactor(annotation.replicationFactor());
        }
        if (annotation.satellite()) {
            options.replicationFactor(ReplicationFactor.ofSatellite());
        }
        final String[] shardKeys = annotation.shardKeys();
        if (shardKeys.length > 0) {
            options.shardKeys(shardKeys);
        }
        if (annotation.numberOfShards() > -1) {
            options.numberOfShards(annotation.numberOfShards());
        }
        if (annotation.allowUserKeys()) {
            options.keyOptions(true, annotation.keyType(),
                    annotation.keyIncrement() > -1 ? annotation.keyIncrement() : null,
                    annotation.keyOffset() > -1 ? annotation.keyOffset() : null);
        }
        return options;
    }

    @Override
    public String getCollection() {
        return expression != null ? expression.getValue(context, String.class) : collection;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        context.setRootObject(applicationContext);
        context.setBeanResolver(new BeanFactoryResolver(applicationContext));
        context.addPropertyAccessor(new BeanFactoryAccessor());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addPersistentProperty(final ArangoPersistentProperty property) {
        super.addPersistentProperty(property);
        if (property.isArangoIdProperty()) {
            if (arangoIdProperty != null) {
                throw new MappingException("Found multiple id indexed properties!");
            }
            arangoIdProperty = property;
        }
        if (property.isRevProperty()) {
            if (revProperty != null) {
                throw new MappingException("Found multiple rev indexed properties!");
            }
            revProperty = property;
        }
        if (property.getTtlIndexed().isPresent()) {
            if (ttlIndexedProperty != null) {
                throw new MappingException("Found multiple ttl indexed properties!");
            }
            ttlIndexedProperty = property;
        }
        property.getComputedValue().ifPresent(i -> computedValueProperties.put(property.getName(), property));
        property.getPersistentIndexed().ifPresent(i -> persistentIndexedProperties.add(property));
        property.getGeoIndexed().ifPresent(i -> geoIndexedProperties.add(property));
        property.getFulltextIndexed().ifPresent(i -> fulltextIndexedProperties.add(property));
    }

    @Override
    public Optional<ArangoPersistentProperty> getArangoIdProperty() {
        return Optional.ofNullable(arangoIdProperty);
    }

    @Override
    public Optional<ArangoPersistentProperty> getRevProperty() {
        return Optional.ofNullable(revProperty);
    }

    @Override
    public CollectionCreateOptions getCollectionOptions() {
        CollectionCreateOptions options;
        if (edgeAnnotation != null) {
            options = createEdgeCollectionOptions(edgeAnnotation);
        } else if (documentAnnotation != null) {
            options = createDocumentCollectionOptions(documentAnnotation);
        } else {
            options = new CollectionCreateOptions().type(CollectionType.DOCUMENT);
        }

        computedValueProperties.values().stream()
                .flatMap(it -> it.getComputedValue()
                        .filter(cv -> !cv.expression().isEmpty())
                        .map(cv -> new com.arangodb.model.ComputedValue()
                                .name(it.getFieldName())
                                .expression(cv.expression())
                                .overwrite(cv.overwrite())
                                .computeOn(cv.computeOn())
                                .keepNull(cv.keepNull())
                                .failOnWarning(cv.failOnWarning())
                        )
                        .stream()
                )
                .forEach(options::computedValues);

        return options;
    }

    @Override
    public Collection<PersistentIndex> getPersistentIndexes() {
        final Collection<PersistentIndex> indexes = getIndexes(PersistentIndex.class);
        Optional.ofNullable(findAnnotation(PersistentIndexes.class))
                .ifPresent(i -> indexes.addAll(Arrays.asList(i.value())));
        return indexes;
    }

    @Override
    public Collection<GeoIndex> getGeoIndexes() {
        final Collection<GeoIndex> indexes = getIndexes(GeoIndex.class);
        Optional.ofNullable(findAnnotation(GeoIndexes.class)).ifPresent(i -> indexes.addAll(Arrays.asList(i.value())));
        return indexes;
    }

    @Override
    @Deprecated
    public Collection<FulltextIndex> getFulltextIndexes() {
        final Collection<FulltextIndex> indexes = getIndexes(FulltextIndex.class);
        Optional.ofNullable(findAnnotation(FulltextIndexes.class))
                .ifPresent(i -> indexes.addAll(Arrays.asList(i.value())));
        return indexes;
    }

    @Override
    public Optional<TtlIndex> getTtlIndex() {
        return getIndex(TtlIndex.class);
    }

    private <A extends Annotation> Optional<A> getIndex(final Class<A> annotation) {
        return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(getType(), annotation));
    }

    public <A extends Annotation> Collection<A> getIndexes(final Class<A> annotation) {
        return findAnnotations(annotation).stream().filter(annotation::isInstance)
                .map(annotation::cast).collect(Collectors.toList());
    }

    @Override
    public Map<String, ArangoPersistentProperty> getComputedValuesProperties() {
        return computedValueProperties;
    }

    @Override
    public Collection<ArangoPersistentProperty> getPersistentIndexedProperties() {
        return persistentIndexedProperties;
    }

    @Override
    public Collection<ArangoPersistentProperty> getGeoIndexedProperties() {
        return geoIndexedProperties;
    }

    @Override
    @Deprecated
    public Collection<ArangoPersistentProperty> getFulltextIndexedProperties() {
        return fulltextIndexedProperties;
    }

    @Override
    public Optional<ArangoPersistentProperty> getTtlIndexedProperty() {
        return Optional.ofNullable(ttlIndexedProperty);
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> Set<A> findAnnotations(final Class<A> annotationType) {
        return (Set<A>) repeatableAnnotationCache.computeIfAbsent(annotationType,
                it -> AnnotatedElementUtils.findMergedRepeatableAnnotations(getType(), it));
    }

    private static class AbsentAccessor extends TargetAwareIdentifierAccessor {

        public AbsentAccessor(final Object target) {
            super(target);
        }

        @Override
        @Nullable
        public Object getIdentifier() {
            return null;
        }
    }

    @Override
    public IdentifierAccessor getArangoIdAccessor(final Object bean) {
        return getArangoIdProperty().isPresent() ? new ArangoIdPropertyIdentifierAccessor(this, bean)
                : new AbsentAccessor(bean);
    }

}
