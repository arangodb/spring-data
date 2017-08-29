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

package com.arangodb.springframework.repository.query.derived;

/**
 * Created by F625633 on 24/07/2017.
 */
public class ConjunctionBuilder {

	private static final String ARRAY_DELIMITER = ", ";
	private static final String PREDICATE_DELIMITER = " AND ";

	private final StringBuilder arrayStringBuilder = new StringBuilder();
	private final StringBuilder predicateStringBuilder = new StringBuilder();

	private int arrays = 0;

	public void add(final PartInformation partInformation) {
		if (partInformation.isArray()) {
			++arrays;
			arrayStringBuilder
					.append((arrayStringBuilder.length() == 0 ? "" : ARRAY_DELIMITER) + partInformation.getClause());
		} else {
			predicateStringBuilder.append(
				(predicateStringBuilder.length() == 0 ? "" : PREDICATE_DELIMITER) + partInformation.getClause());
		}
	}

	private String buildArrayString() {
		if (arrays > 1) {
			return "INTERSECTION(" + arrayStringBuilder.toString() + ")";
		}
		return arrayStringBuilder.toString();
	}

	public Conjunction build() {
		return new Conjunction(buildArrayString(), predicateStringBuilder.toString());
	}
}
