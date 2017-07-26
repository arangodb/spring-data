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

import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import com.arangodb.entity.CollectionType;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Edge;

/**
 * @author Mark Vollmary
 * @param <T>
 *
 */
public class DefaultArangoPersistentEntity<T> extends BasicPersistentEntity<T, ArangoPersistentProperty>
		implements ArangoPersistentEntity<T> {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private String collection;
	private final StandardEvaluationContext context;
	private final PersistentPropertyAccessorFactory propertyAccessorFactory;

	private ArangoPersistentProperty keyProperty;
	private ArangoPersistentProperty revProperty;

	private CollectionCreateOptions collectionOptions;

	public DefaultArangoPersistentEntity(final TypeInformation<T> information) {
		super(information);
		collection = StringUtils.uncapitalize(information.getType().getSimpleName());
		context = new StandardEvaluationContext();
		final Optional<Document> document = Optional.ofNullable(findAnnotation(Document.class));
		final Optional<Edge> edge = Optional.ofNullable(findAnnotation(Edge.class));
		if (document.isPresent()) {
			final Document d = document.get();
			collection = StringUtils.hasText(d.value()) ? d.value() : collection;
			collectionOptions = createCollectionOptions(d);
		} else if (edge.isPresent()) {
			final Edge e = edge.get();
			collection = StringUtils.hasText(e.value()) ? e.value() : collection;
			collectionOptions = createCollectionOptions(e);
		}
		final Expression expression = PARSER.parseExpression(collection, ParserContext.TEMPLATE_EXPRESSION);
		if (expression != null) {
			collection = expression.getValue(context, String.class);
		}
		this.propertyAccessorFactory = BeanWrapperPropertyAccessorFactory.INSTANCE;
	}

	private static CollectionCreateOptions createCollectionOptions(final Document annotation) {
		final CollectionCreateOptions options = new CollectionCreateOptions().type(CollectionType.DOCUMENT)
				.waitForSync(annotation.waitForSync()).doCompact(annotation.doCompact())
				.isVolatile(annotation.isVolatile()).isSystem(annotation.isSystem());
		if (annotation.journalSize() > -1) {
			options.journalSize(annotation.journalSize());
		}
		if (annotation.replicationFactor() > -1) {
			options.replicationFactor(annotation.replicationFactor());
		}
		final String[] shardKeys = annotation.shardKeys();
		if (shardKeys.length > 1 || (shardKeys.length > 0 && StringUtils.hasText(shardKeys[0]))) {
			options.shardKeys(shardKeys);
		}
		if (annotation.numberOfShards() > -1) {
			options.numberOfShards(annotation.numberOfShards());
		}
		if (annotation.indexBuckets() > -1) {
			options.indexBuckets(annotation.indexBuckets());
		}
		if (annotation.allowUserKeys()) {
			options.keyOptions(annotation.allowUserKeys(), annotation.keyType(),
				annotation.keyIncrement() > -1 ? annotation.keyIncrement() : null,
				annotation.keyOffset() > -1 ? annotation.keyOffset() : null);
		}
		return options;
	}

	private static CollectionCreateOptions createCollectionOptions(final Edge annotation) {
		final CollectionCreateOptions options = new CollectionCreateOptions().type(CollectionType.DOCUMENT)
				.waitForSync(annotation.waitForSync()).doCompact(annotation.doCompact())
				.isVolatile(annotation.isVolatile()).isSystem(annotation.isSystem());
		if (annotation.journalSize() > -1) {
			options.journalSize(annotation.journalSize());
		}
		if (annotation.replicationFactor() > -1) {
			options.replicationFactor(annotation.replicationFactor());
		}
		final String[] shardKeys = annotation.shardKeys();
		if (shardKeys.length > 1 || (shardKeys.length > 0 && StringUtils.hasText(shardKeys[0]))) {
			options.shardKeys(shardKeys);
		}
		if (annotation.numberOfShards() > -1) {
			options.numberOfShards(annotation.numberOfShards());
		}
		if (annotation.indexBuckets() > -1) {
			options.indexBuckets(annotation.indexBuckets());
		}
		if (annotation.allowUserKeys()) {
			options.keyOptions(annotation.allowUserKeys(), annotation.keyType(),
				annotation.keyIncrement() > -1 ? annotation.keyIncrement() : null,
				annotation.keyOffset() > -1 ? annotation.keyOffset() : null);
		}
		return options;
	}

	@Override
	public String getCollection() {
		return collection;
	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
		context.setRootObject(applicationContext);
		context.setBeanResolver(new BeanFactoryResolver(applicationContext));
		context.addPropertyAccessor(new BeanFactoryAccessor());
	}

	@Override
	public PersistentPropertyAccessor getPropertyAccessor(final Object source) {
		return propertyAccessorFactory.getPropertyAccessor(this, source);
	}

	@Override
	public void addPersistentProperty(final ArangoPersistentProperty property) {
		super.addPersistentProperty(property);
		if (property.isKeyProperty()) {
			keyProperty = property;
		}
		if (property.isRevProperty()) {
			revProperty = property;
		}
	}

	@Override
	public Optional<ArangoPersistentProperty> getKeyProperty() {
		return Optional.ofNullable(keyProperty);
	}

	@Override
	public Optional<ArangoPersistentProperty> getRevProperty() {
		return Optional.ofNullable(revProperty);
	}

	@Override
	public CollectionCreateOptions getCollectionOptions() {
		return collectionOptions;
	}
}
