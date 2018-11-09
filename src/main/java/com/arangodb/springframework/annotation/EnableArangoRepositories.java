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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;

import com.arangodb.springframework.repository.ArangoRepositoriesRegistrar;
import com.arangodb.springframework.repository.ArangoRepositoryFactoryBean;

/**
 * Annotation to activate ArangoDB repositories.
 * <p>
 * If no base package is configured through either {@link #value},
 * {@link #basePackages} or {@link #basePackageClasses} it will trigger scanning
 * of the package of annotated class.
 *
 * @author Audrius Malele
 * @author Mark McCormick
 * @author Mark Vollmary
 * @author Christian Lechner
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(ArangoRepositoriesRegistrar.class)
public @interface EnableArangoRepositories {

	/**
	 * Alias for {@link #basePackages}.
	 * <p>
	 * Intended to be used instead of {@link #basePackages} when no other attributes
	 * are needed &mdash; for example:
	 * {@code @EnableArangoRepositories("org.my.project")} instead of
	 * {@code @EnableArangoRepositories(basePackages = "org.my.project")}.
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components.
	 * <p>
	 * Use {@link #basePackageClasses} for a type-safe alternative to package names.
	 */
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages} for specifying the packages to
	 * scan for annotated components.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Specifies which types are eligible for component scanning. Further narrows
	 * the set of candidate components from everything in {@link #basePackages} to
	 * everything in the base packages that matches the given filter or filters.
	 */
	ComponentScan.Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for component scanning.
	 */
	ComponentScan.Filter[] excludeFilters() default {};

	/**
	 * Returns the postfix to be used for custom repository implementations. Defaults to {@literal Impl}.
	 */
	String repositoryImplementationPostfix() default "Impl";

	/**
	 * Returns the {@link FactoryBean} class to be used for each repository
	 * instance. Defaults to {@link ArangoRepositoryFactoryBean}.
	 */
	Class<?> repositoryFactoryBeanClass() default ArangoRepositoryFactoryBean.class;

	/**
	 * Configures the location of the Spring Data named queries properties file. Defaults to
	 * {@code META-INF/arango-named-queries.properties}.
	 */
	String namedQueriesLocation() default "";

	/**
	 * Returns the key of the {@link QueryLookupStrategy} that should be used to lookup queries for query methods.
	 * Currently only the default {@link Key#CREATE_IF_NOT_FOUND} is supported.
	 */
	Key queryLookupStrategy() default Key.CREATE_IF_NOT_FOUND;

}
