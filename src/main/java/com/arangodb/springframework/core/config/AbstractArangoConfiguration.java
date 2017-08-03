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

package com.arangodb.springframework.core.config;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;

import com.arangodb.ArangoDB;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.annotation.To;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.ArangoCustomConversions;
import com.arangodb.springframework.core.convert.CustomConversions;
import com.arangodb.springframework.core.convert.DefaultArangoConverter;
import com.arangodb.springframework.core.convert.resolver.FromResolver;
import com.arangodb.springframework.core.convert.resolver.RefResolver;
import com.arangodb.springframework.core.convert.resolver.ReferenceResolver;
import com.arangodb.springframework.core.convert.resolver.RelationResolver;
import com.arangodb.springframework.core.convert.resolver.RelationsResolver;
import com.arangodb.springframework.core.convert.resolver.ResolverFactory;
import com.arangodb.springframework.core.convert.resolver.ToResolver;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.template.ArangoTemplate;
import com.arangodb.velocypack.module.jdk8.VPackJdk8Module;
import com.arangodb.velocypack.module.joda.VPackJodaModule;

/**
 * @author Mark Vollmary
 *
 */
@Configuration
public abstract class AbstractArangoConfiguration {

	@Bean
	public abstract ArangoDB.Builder arango();

	@Bean
	public abstract String database();

	private ArangoDB.Builder configure(final ArangoDB.Builder arango) {
		return arango.registerModules(new VPackJdk8Module(), new VPackJodaModule());
	}

	@Bean
	public ArangoOperations arangoTemplate() throws Exception {
		return new ArangoTemplate(configure(arango()), database(), arangoConverter());
	}

	@Bean
	public ArangoMappingContext arangoMappingContext() throws Exception {
		final ArangoMappingContext context = new ArangoMappingContext();
		context.setInitialEntitySet(getInitialEntitySet());
		context.setFieldNamingStrategy(fieldNamingStrategy());
		context.setSimpleTypeHolder(customConversions().getSimpleTypeHolder());
		return context;
	}

	@Bean
	public CustomConversions customConversions() {
		return new ArangoCustomConversions(Collections.emptyList());
	}

	private Set<? extends Class<?>> getInitialEntitySet() throws ClassNotFoundException {
		return ArangoEntityClassScanner.scanForEntities(getEntityBasePackages());
	}

	protected String[] getEntityBasePackages() {
		return new String[] { getClass().getPackage().getName() };
	}

	protected FieldNamingStrategy fieldNamingStrategy() {
		return PropertyNameFieldNamingStrategy.INSTANCE;
	}

	@Bean
	public ArangoConverter arangoConverter() throws Exception {
		return new DefaultArangoConverter(arangoMappingContext(), customConversions(), new ResolverFactory() {
			@SuppressWarnings("unchecked")
			@Override
			public <A extends Annotation> Optional<ReferenceResolver<A>> getReferenceResolver(final A annotation) {
				ReferenceResolver<A> resolver = null;
				try {
					if (annotation instanceof Ref) {
						resolver = (ReferenceResolver<A>) new RefResolver(arangoTemplate());
					}
				} catch (final Exception e) {
					// TODO
				}
				return Optional.ofNullable(resolver);
			}

			@SuppressWarnings("unchecked")
			@Override
			public <A extends Annotation> Optional<RelationResolver<A>> getRelationResolver(final A annotation) {
				RelationResolver<A> resolver = null;
				try {
					if (annotation instanceof From) {
						resolver = (RelationResolver<A>) new FromResolver(arangoTemplate());
					} else if (annotation instanceof To) {
						resolver = (RelationResolver<A>) new ToResolver(arangoTemplate());
					} else if (annotation instanceof Relations) {
						resolver = (RelationResolver<A>) new RelationsResolver(arangoTemplate());
					}
				} catch (final Exception e) {
					// TODO
				}
				return Optional.ofNullable(resolver);
			}
		});
	}

}
