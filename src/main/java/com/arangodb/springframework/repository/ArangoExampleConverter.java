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

package com.arangodb.springframework.repository;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.util.Assert;

import com.arangodb.springframework.core.convert.resolver.ReferenceResolver;
import com.arangodb.springframework.core.convert.resolver.ResolverFactory;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;

/**
 * Converts Example to String representing predicate expression and puts
 * necessary bindings in the given bindVars Map
 */
public class ArangoExampleConverter<T> {

	private final ArangoMappingContext context;
	private final ResolverFactory resolverFactory;

	public ArangoExampleConverter(final ArangoMappingContext context, final ResolverFactory resolverFactory) {
		this.context = context;
		this.resolverFactory = resolverFactory;
	}

	public String convertExampleToPredicate(final Example<T> example, final Map<String, Object> bindVars) {
		final StringBuilder predicateBuilder = new StringBuilder();
		final ArangoPersistentEntity<?> persistentEntity = context.getPersistentEntity(example.getProbeType());
		Assert.isTrue(example.getProbe() != null, "Probe in Example cannot be null");
		final String bindEntintyName = "e";
		traversePropertyTree(example, predicateBuilder, bindVars, "", "", persistentEntity, example.getProbe(),
				bindEntintyName);
		return predicateBuilder.toString();
	}

	private void traversePropertyTree(final Example<T> example, final StringBuilder predicateBuilder,
			final Map<String, Object> bindVars, final String path, final String javaPath,
			final ArangoPersistentEntity<?> entity, final Object object, final String bindEntintyName) {
		final PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(object);
		entity.doWithProperties((final ArangoPersistentProperty property) -> {
			if (property.getFrom().isPresent() || property.getTo().isPresent() || property.getRelations().isPresent()) {
				return;
			}
			final String fullPath = path + (path.length() == 0 ? "" : ".") + property.getFieldName();
			final String fullJavaPath = javaPath + (javaPath.length() == 0 ? "" : ".") + property.getName();
			final Object value = accessor.getProperty(property);

			if (property.isCollectionLike() && value != null) {
				final ArangoPersistentEntity<?> persistentEntity = context
						.getPersistentEntity(property.getActualType());
				final StringBuilder predicateBuilderArray = new StringBuilder();
				for (Object item : (Iterable) value) {
					final StringBuilder predicateBuilderArrayItem = new StringBuilder();
					traversePropertyTree(example, predicateBuilderArrayItem, bindVars, "", fullJavaPath,
							persistentEntity, item, "CURRENT");
					if (predicateBuilderArray.length() > 0) {
						predicateBuilderArray.append(" OR ");
					}
					predicateBuilderArray.append(predicateBuilderArrayItem.toString());
				}
				final String delimiter = example.getMatcher().isAllMatching() ? " AND " : " OR ";
				if (predicateBuilder.length() > 0) {
					predicateBuilder.append(delimiter);
				}
				String clause = String.format("LENGTH(%s.%s[* FILTER %s ])>0", bindEntintyName, property.getName(),
						predicateBuilderArray.toString());
				predicateBuilder.append(clause);

			} else if (property.isEntity() && value != null) {
				final ArangoPersistentEntity<?> persistentEntity = context.getPersistentEntity(property.getType());
				traversePropertyTree(example, predicateBuilder, bindVars, fullPath, fullJavaPath, persistentEntity,
						value, bindEntintyName);
			} else if (!example.getMatcher().isIgnoredPath(fullJavaPath) && (value != null
					|| example.getMatcher().getNullHandler().equals(ExampleMatcher.NullHandler.INCLUDE))) {
				addPredicate(example, predicateBuilder, bindVars, fullPath, fullJavaPath, value, bindEntintyName);
			}
		});

		entity.doWithAssociations((AssociationHandler<ArangoPersistentProperty>) association -> {

			final ArangoPersistentProperty property = association.getInverse();
			final String fullPath = path + (path.length() == 0 ? "" : ".") + property.getFieldName();
			final String fullJavaPath = javaPath + (javaPath.length() == 0 ? "" : ".") + property.getName();
			final Object value = accessor.getProperty(property);

			if (property.isEntity() && property.isAssociation() && value != null) {
				final ArangoPersistentEntity<?> persistentEntity = context.getPersistentEntity(property.getType());
				final PersistentPropertyAccessor<?> associatedAccessor = persistentEntity.getPropertyAccessor(value);
				final Object idValue = associatedAccessor.getProperty(persistentEntity.getIdProperty());
				String refIdValue = null;
				if (property.getRef().isPresent()) {
					final Optional<ReferenceResolver<Annotation>> resolver = resolverFactory
							.getReferenceResolver(property.getRef().get());
					refIdValue = resolver.get().write(value, persistentEntity, idValue, property.getRef().get());
				} else {
					refIdValue = String.format("%s/%s", persistentEntity.getCollection(), idValue);
				}
				addPredicate(example, predicateBuilder, bindVars, fullPath, fullJavaPath, refIdValue, bindEntintyName);
			}
		});
	}

	private void addPredicate(final Example<T> example, final StringBuilder predicateBuilder,
			final Map<String, Object> bindVars, final String fullPath, final String fullJavaPath, Object value,
			final String bindEntintyName) {
		final String delimiter = example.getMatcher().isAllMatching() ? " AND " : " OR ";
		if (predicateBuilder.length() > 0) {
			predicateBuilder.append(delimiter);
		}
		final String binding = Integer.toString(bindVars.size());
		String clause;
		final ExampleMatcher.PropertySpecifier specifier = example.getMatcher().getPropertySpecifiers()
				.getForPath(fullPath);
		if (specifier != null && value != null) {
			value = specifier.transformValue(Optional.of(value)).orElse(null);
		}
		if (value == null) {
			clause = String.format("%s.%s == null", bindEntintyName, fullPath);
		} else if (String.class.isAssignableFrom(value.getClass())) {
			final boolean ignoreCase = specifier == null ? example.getMatcher().isIgnoreCaseEnabled()
					: (specifier.getIgnoreCase() == null ? false : specifier.getIgnoreCase());
			final ExampleMatcher.StringMatcher stringMatcher = (specifier == null
					|| specifier.getStringMatcher() == ExampleMatcher.StringMatcher.DEFAULT)
							? example.getMatcher().getDefaultStringMatcher()
							: specifier.getStringMatcher();
			final String string = (String) value;
			if (stringMatcher == ExampleMatcher.StringMatcher.REGEX) {
				clause = String.format("REGEX_TEST(%s.%s, @%s, %b)", bindEntintyName, fullPath, binding, ignoreCase);
			} else {
				clause = String.format("LIKE(%s.%s, @%s, %b)", bindEntintyName, fullPath, binding, ignoreCase);
			}
			switch (stringMatcher) {
			case STARTING:
				value = escape(string) + "%";
				break;
			case ENDING:
				value = "%" + escape(string);
				break;
			case CONTAINING:
				value = "%" + escape(string) + "%";
				break;
			case REGEX:
				value = escape(string);
				break;
			case DEFAULT:
			case EXACT:
			default:
				value = escape(string);
				break;
			}
		} else {
			clause = String.format("%s.%s == @%s", bindEntintyName, fullPath, binding);
		}
		predicateBuilder.append(clause);
		if (value != null) {
			bindVars.put(binding, value);
		}
	}

	private static final Set<Character> SPECIAL_CHARACTERS = new HashSet<>();

	static {
		SPECIAL_CHARACTERS.add('\\');
		SPECIAL_CHARACTERS.add('_');
		SPECIAL_CHARACTERS.add('%');
	}

	private static String escape(final String string) {
		final StringBuilder stringBuilder = new StringBuilder();
		for (final Character character : string.toCharArray()) {
			if (SPECIAL_CHARACTERS.contains(character)) {
				stringBuilder.append('\\');
			}
			stringBuilder.append(character);
		}
		return stringBuilder.toString();
	}
}
