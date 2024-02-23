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

import com.arangodb.model.ComputedValue;

import java.lang.annotation.*;

/**
 * Annotation to define computed values of a collection.
 *
 * @see <a href="https://docs.arangodb.com/stable/concepts/data-structure/documents/computed-values">Reference Doc</a>
 */
@Repeatable(ArangoComputedValueDefinitions.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ArangoComputedValueDefinition {

    /**
     * @return The name of the target attribute. Can only be a top-level attribute, but you may return a nested object.
     * Cannot be _key, _id, _rev, _from, _to, or a shard key attribute.
     * @see ComputedValue#name(String)
     */
    String name() default "";

    /**
     * @return An AQL RETURN operation with an expression that computes the desired value. If empty, the computed value
     * data definition will not be set on collection creation.
     * @see ComputedValue#expression(String)
     */
    String expression() default "";

    /**
     * @return Whether the computed value shall take precedence over a user-provided or existing attribute.
     * The default is false.
     * @see ComputedValue#overwrite(Boolean)
     */
    boolean overwrite() default false;

    /**
     * @return An array of operations to define on which write operations the value shall be computed.
     * The default is ["insert", "update", "replace"].
     * @see ComputedValue#computeOn(ComputedValue.ComputeOn...)
     */
    ComputedValue.ComputeOn[] computeOn() default {
            ComputedValue.ComputeOn.insert,
            ComputedValue.ComputeOn.update,
            ComputedValue.ComputeOn.replace
    };

    /**
     * @return Whether the target attribute shall be set if the expression evaluates to null. You can set the option to
     * false to not set (or unset) the target attribute if the expression returns null. The default is true.
     * @see ComputedValue#keepNull(Boolean)
     */
    boolean keepNull() default true;

    /**
     * @return Whether to let the write operation fail if the expression produces a warning.
     * The default is false.
     * @see ComputedValue#failOnWarning(Boolean)
     */
    boolean failOnWarning() default false;
}
