package com.arangodb.springframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface QueryOptions {

    int batchSize() default -1;

    boolean cache() default false;

    boolean count() default false;

    boolean fullCount() default false;

    int maxPlans() default -1;

    boolean profile() default false;

    String[] rules() default {};

    int ttl() default -1;
}
