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

package com.arangodb.springframework.config;

import java.util.Collections;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;

import com.arangodb.ArangoDB;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.convert.ArangoCustomConversions;
import com.arangodb.springframework.core.convert.ArangoTypeMapper;
import com.arangodb.springframework.core.convert.DefaultArangoConverter;
import com.arangodb.springframework.core.convert.DefaultArangoTypeMapper;
import com.arangodb.springframework.core.convert.resolver.DefaultResolverFactory;
import com.arangodb.springframework.core.convert.resolver.ResolverFactory;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.template.ArangoTemplate;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 *
 */
@Configuration
public abstract class AbstractArangoConfiguration {

	protected abstract ArangoDB.Builder arango();

	protected abstract String database();

	@Bean
	public ArangoOperations arangoTemplate() throws Exception {
		return new ArangoTemplate(arango().build(), database(), arangoConverter());
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
	public ArangoConverter arangoConverter() throws Exception {
		return new DefaultArangoConverter(arangoMappingContext(), customConversions(), resolverFactory(),
				arangoTypeMapper());
	}

	protected CustomConversions customConversions() {
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

	protected String typeKey() {
		return DefaultArangoTypeMapper.DEFAULT_TYPE_KEY;
	}

	protected ArangoTypeMapper arangoTypeMapper() throws Exception {
		return new DefaultArangoTypeMapper(typeKey(), arangoMappingContext());
	}

	protected ResolverFactory resolverFactory() throws Exception {
		return new DefaultResolverFactory(arangoTemplate());
	}

}
