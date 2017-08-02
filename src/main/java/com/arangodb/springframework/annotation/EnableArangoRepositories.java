package com.arangodb.springframework.annotation;

import com.arangodb.springframework.core.repository.ArangoRepositoriesRegistrar;
import com.arangodb.springframework.core.repository.ArangoRepositoryFactoryBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Created by F625633 on 07/07/2017.
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
	String repositoryImplementationPostfix() default "";
	Class<?> repositoryFactoryBeanClass() default ArangoRepositoryFactoryBean.class;
	String namedQueriesLocation() default "";
}
