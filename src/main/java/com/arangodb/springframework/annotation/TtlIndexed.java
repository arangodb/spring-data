package com.arangodb.springframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a field to be indexed using ArangoDB's Ttl index.
 *
 * @author Dmitry Krasaev
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TtlIndexed {

    /**
     * The time interval (in seconds) from the point in time of the value of the annotated field after which the
     * documents count as expired. Default is 0, which means that the documents expire as soon as the server time passes
     * the point in time stored in the document attribute.
     */
    int expireAfter() default 0;

}
