/**
 *
 */
package com.arangodb.springframework.config;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.annotation.To;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.ArangoCustomConversions;
import com.arangodb.springframework.core.convert.ArangoTypeMapper;
import com.arangodb.springframework.core.convert.DefaultArangoConverter;
import com.arangodb.springframework.core.convert.DefaultArangoTypeMapper;
import com.arangodb.springframework.core.convert.resolver.DocumentFromResolver;
import com.arangodb.springframework.core.convert.resolver.DocumentToResolver;
import com.arangodb.springframework.core.convert.resolver.EdgeFromResolver;
import com.arangodb.springframework.core.convert.resolver.EdgeToResolver;
import com.arangodb.springframework.core.convert.resolver.RefResolver;
import com.arangodb.springframework.core.convert.resolver.ReferenceResolver;
import com.arangodb.springframework.core.convert.resolver.RelationResolver;
import com.arangodb.springframework.core.convert.resolver.RelationsResolver;
import com.arangodb.springframework.core.convert.resolver.ResolverFactory;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.template.ArangoTemplate;

/**
 * Defines methods to customize the Java-based configuration for Spring Data
 * ArangoDB.
 * 
 * @author Mark Vollmary
 *
 */
public interface ArangoConfiguration {

	ArangoDB.Builder arango();

	String database();

	@Bean
	default ArangoOperations arangoTemplate() throws Exception {
		return new ArangoTemplate(arango().build(), database(), arangoConverter(), resolverFactory());
	}

	@Bean
	default ArangoMappingContext arangoMappingContext() throws Exception {
		final ArangoMappingContext context = new ArangoMappingContext();
		context.setInitialEntitySet(getInitialEntitySet());
		context.setFieldNamingStrategy(fieldNamingStrategy());
		context.setSimpleTypeHolder(customConversions().getSimpleTypeHolder());
		return context;
	}

	@Bean
	default ArangoConverter arangoConverter() throws Exception {
		return new DefaultArangoConverter(arangoMappingContext(), customConversions(), resolverFactory(),
				arangoTypeMapper());
	}

	default CustomConversions customConversions() {
		return new ArangoCustomConversions(customConverters());
	}

	default Collection<Converter<?, ?>> customConverters() {
		return Collections.emptyList();
	}

	default Set<? extends Class<?>> getInitialEntitySet() throws ClassNotFoundException {
		return ArangoEntityClassScanner.scanForEntities(getEntityBasePackages());
	}

	default String[] getEntityBasePackages() {
		return new String[] { getClass().getPackage().getName() };
	}

	default FieldNamingStrategy fieldNamingStrategy() {
		return PropertyNameFieldNamingStrategy.INSTANCE;
	}

	default String typeKey() {
		return DefaultArangoTypeMapper.DEFAULT_TYPE_KEY;
	}

	default ArangoTypeMapper arangoTypeMapper() throws Exception {
		return new DefaultArangoTypeMapper(typeKey(), arangoMappingContext());
	}

	default ResolverFactory resolverFactory() {
		return new ResolverFactory() {
			@SuppressWarnings("unchecked")
			@Override
			public <A extends Annotation> Optional<ReferenceResolver<A>> getReferenceResolver(final A annotation) {
				ReferenceResolver<A> resolver = null;
				try {
					if (annotation instanceof Ref) {
						resolver = (ReferenceResolver<A>) new RefResolver(arangoTemplate());
					}
				} catch (final Exception e) {
					throw new ArangoDBException(e);
				}
				return Optional.ofNullable(resolver);
			}

			@SuppressWarnings("unchecked")
			@Override
			public <A extends Annotation> Optional<RelationResolver<A>> getRelationResolver(final A annotation,
					final Class<? extends Annotation> collectionType) {
				RelationResolver<A> resolver = null;
				try {
					if (annotation instanceof From) {
						if (collectionType == Edge.class) {
							resolver = (RelationResolver<A>) new EdgeFromResolver(arangoTemplate());
						} else if (collectionType == Document.class) {
							resolver = (RelationResolver<A>) new DocumentFromResolver(arangoTemplate());
						}
					} else if (annotation instanceof To) {
						if (collectionType == Edge.class) {
							resolver = (RelationResolver<A>) new EdgeToResolver(arangoTemplate());
						} else if (collectionType == Document.class) {
							resolver = (RelationResolver<A>) new DocumentToResolver(arangoTemplate());
						}
					} else if (annotation instanceof Relations) {
						resolver = (RelationResolver<A>) new RelationsResolver(arangoTemplate());
					}
				} catch (final Exception e) {
					throw new ArangoDBException(e);
				}
				return Optional.ofNullable(resolver);
			}
		};
	}

}