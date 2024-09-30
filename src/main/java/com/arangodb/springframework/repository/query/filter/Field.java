/*
 * DISCLAIMER
 *
 * Copyright 2023 Hewlett Packard Enterprise Development LP.
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
 * Copyright holder is Hewlett Packard Enterprise Development LP.
 */
package com.arangodb.springframework.repository.query.filter;

import com.arangodb.springframework.core.util.AqlUtils;

public class Field {
    final String collection;
    final String name;

    public Field(String collection, String name) {
        this.collection = collection;
        this.name = name;
    }

    public static Field of(String collection, String name) {
        return new Field(collection, name);
    }

    public Filterable eq(String right) {
        return new CompareExpression(Comparator.EQ, this, right);
    }

    @Override
    public String toString() {
        return AqlUtils.escapeProperty(String.format("%s.%s", collection, name));
    }

}
