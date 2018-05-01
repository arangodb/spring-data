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

package com.arangodb.springframework.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;

import com.arangodb.springframework.repository.ArangoRepositoriesRegistrar;
import com.arangodb.springframework.repository.ArangoRepositoryFactoryBean;

/**
 * 
 * @author Andrew Fleming
 * @author Mark Vollmary
 * @author Christian Lechner
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(ArangoRepositoriesRegistrar.class)
public @interface EnableArangoRepositories {

	String[] value() default {};

	String[] basePackages() default {};

	Class<?>[] basePackageClasses() default {};

	ComponentScan.Filter[] includeFilters() default {};

	ComponentScan.Filter[] excludeFilters() default {};

	/**
	 * Returns the postfix to be used for custom repository implementations. Defaults to {@literal Impl}.
	 */
	String repositoryImplementationPostfix() default "Impl";

	Class<?> repositoryFactoryBeanClass() default ArangoRepositoryFactoryBean.class;

	/**
	 * Configures the location of the Spring Data named queries properties file. Defaults to
	 * {@code META-INFO/arango-named-queries.properties}.
	 */
	String namedQueriesLocation() default "";

	/**
	 * Returns the key of the {@link QueryLookupStrategy} that should be used to lookup queries for query methods.
	 * Currently only the default {@link Key#CREATE_IF_NOT_FOUND} is supported.
	 */
	Key queryLookupStrategy() default Key.CREATE_IF_NOT_FOUND;

}
