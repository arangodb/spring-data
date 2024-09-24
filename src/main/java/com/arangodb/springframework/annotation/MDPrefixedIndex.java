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

import com.arangodb.model.MDIFieldValueTypes;

import java.lang.annotation.*;

/**
 * Annotation to define given fields to be indexed using ArangoDB's Multi-Dimensional Prefixed index.
 */
@Repeatable(MDPrefixedIndexes.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface MDPrefixedIndex {

    /**
     * A list of attribute paths
     */
    String[] fields();

    /**
     * Attribute names used as search prefix
     */
    String[] prefixFields();

    /**
     * If {@literal true}, then create a unique index
     */
    boolean unique() default false;

    /**
     * must be {@link MDIFieldValueTypes#DOUBLE}, currently only doubles are supported as values
     */
    MDIFieldValueTypes fieldValueTypes() default MDIFieldValueTypes.DOUBLE;

    /**
     * If {@literal true}, then create a sparse index
     */
    boolean sparse() default false;

}
