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

package com.arangodb.springframework;

import com.arangodb.ArangoDB;
import com.arangodb.config.ArangoConfigProperties;
import com.arangodb.springframework.annotation.EnableArangoAuditing;
import com.arangodb.springframework.annotation.EnableArangoRepositories;
import com.arangodb.springframework.config.ArangoConfiguration;
import com.arangodb.springframework.core.mapping.CustomMappingTest;
import com.arangodb.springframework.testdata.Person;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.AuditorAware;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Mark Vollmary
 * @author Christian Lechner
 */
@Configuration
@ComponentScan({ "com.arangodb.springframework.component", "com.arangodb.springframework.core.mapping.event" })
@EnableArangoRepositories(basePackages = {
		"com.arangodb.springframework.repository",
		"com.arangodb.springframework.example.polymorphic.repository",
		"com.arangodb.springframework.debug.repository"},
						  namedQueriesLocation = "classpath*:arango-named-queries-test.properties")
@EnableArangoAuditing(auditorAwareRef = "auditorProvider")
public class ArangoTestConfiguration implements ArangoConfiguration {

	public static final String DB = "spring-test-db";

	@Override
	public ArangoDB.Builder arango() {
		return new ArangoDB.Builder().loadProperties(ArangoConfigProperties.fromFile());
	}

	@Override
	public String database() {
		return DB;
	}

	@Override
	public Collection<Converter<?, ?>> customConverters() {
		final Collection<Converter<?, ?>> converters = new ArrayList<>();
		converters.add(new CustomMappingTest.CustomJsonNodeReadTestConverter());
		converters.add(new CustomMappingTest.CustomJsonNodeWriteTestConverter());
		converters.add(new CustomMappingTest.CustomDBEntityReadTestConverter());
		converters.add(new CustomMappingTest.CustomDBEntityWriteTestConverter());
		return converters;
	}

	@Bean
	public AuditorAware<Person> auditorProvider() {
		return new AuditorProvider();
	}
}
