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

import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractExpression implements Filterable {
    @Override
    public Filterable and(Filterable other) {
        return new CombinedExpression(Operator.AND, this, other);
    }

    @Override
    public Filterable or(Filterable other) {
        return new CombinedExpression(Operator.OR, this, other);
    }

    @Override
    public Filterable group() {
        return new GroupExpression(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("");

        if (this instanceof CompareExpression) {
            sb.append(((CompareExpression) this).toString());
        } else if (this instanceof GroupExpression) {
            final GroupExpression filter = (GroupExpression) this;
            final List<String> expressions = filter.expressions.stream().map(expr -> expr.toString()).collect(Collectors.toList());
            final String mergedExpr = (filter.hasSingleCondition()) ? expressions.get(0): String.join(filter.operator.surroundedWithSpaces(), expressions);

            sb.append(String.format("(%s)", mergedExpr));
        } else if (this instanceof CombinedExpression) {
            final CombinedExpression filter = (CombinedExpression) this;
            final List<String> expressions = filter.expressions.stream().map(expr -> expr.toString()).collect(Collectors.toList());

            sb.append(String.join(filter.operator.surroundedWithSpaces(), expressions));
        }

        return sb.toString();
    }
}
