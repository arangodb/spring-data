package com.arangodb.springframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a field to be indexed using ArangoDB's Ttl
 * index.
 *
 * @author Dmitry Krasaev
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TtlIndexed {

    /**
     * The time (in seconds) after a document’s creation after which the documents count as "expired"
     * Default is 0, means immediately when wall clock time reaches the value specified in a field
     */
    int expireAfter() default 0;

}
