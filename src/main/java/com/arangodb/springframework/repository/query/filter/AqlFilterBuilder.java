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

public class AqlFilterBuilder {
    final Filterable filter;

    private AqlFilterBuilder(final Filterable filter) {
        this.filter = filter;
    }

    public static AqlFilterBuilder of(final Filterable filter) {
        return new AqlFilterBuilder(filter);
    }

    public String toFilterStatement() {
        if (filter == null) {
            return "";
        }

        return String.format("FILTER %s", filter);
    }

    String parseFilter() {
        StringBuilder sb = new StringBuilder("");

        if (this.filter instanceof CompareExpression) {
            sb.append(((CompareExpression) this.filter).toString());
        } else if (this.filter instanceof GroupExpression) {
            final GroupExpression filter = (GroupExpression) this.filter;
            final List<String> conditions = filter.expressions.stream().map(f -> f.toString()).collect(Collectors.toList());
            final String expression = (filter.hasSingleCondition()) ? conditions.get(0): String.join(filter.operator.surroundedWithSpaces(), conditions);

            sb.append(String.format("(%s)", expression));
        } else if (this.filter instanceof CombinedExpression) {
            final CombinedExpression filter = (CombinedExpression) this.filter;
            final List<String> conditions = filter.expressions.stream().map(f -> f.toString()).collect(Collectors.toList());

            sb.append(String.join(filter.operator.surroundedWithSpaces(), conditions));
        }

        return sb.toString();
    }
}
