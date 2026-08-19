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

import java.util.ArrayList;
import java.util.List;

public class CombinedExpression extends AbstractExpression {

    final Operator operator;
    final List<Filterable> expressions;

    public CombinedExpression(Operator operator, Filterable left, Filterable right) {
        this.operator = operator;
        this.expressions = new ArrayList<Filterable>() {
            {
                add(left);
            }
        };

        if (right != null) {
            expressions.add(right);
        }
    }
}
