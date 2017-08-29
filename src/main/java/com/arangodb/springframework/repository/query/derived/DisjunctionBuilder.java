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

import java.util.LinkedList;

/**
 * Created by F625633 on 24/07/2017.
 */
public class DisjunctionBuilder {

	private static final String ARRAY_DELIMITER = ", ";
	private static final String PREDICATE_DELIMITER = " OR ";
	private static final String SUBQUERY_TEMPLATE = "(FOR e in %s FILTER %s RETURN %s)";

	private final DerivedQueryCreator queryCreator;

	private final LinkedList<Conjunction> conjunctions = new LinkedList<>();

	private final StringBuilder arrayStringBuilder = new StringBuilder();
	private final StringBuilder predicateStringBuilder = new StringBuilder();

	private int arrays = 0;

	public DisjunctionBuilder(final DerivedQueryCreator queryCreator) {
		this.queryCreator = queryCreator;
	}

	public void add(final Conjunction conjunction) {
		conjunctions.add(conjunction);
		if (conjunction.isArray()) {
			++arrays;
			final String array = conjunction.hasPredicate()
					? String.format(SUBQUERY_TEMPLATE, conjunction.getArray(), conjunction.getPredicate(), "e")
					: conjunction.getArray();
			arrayStringBuilder.append((arrayStringBuilder.length() == 0 ? "" : ARRAY_DELIMITER) + array);
		} else {
			predicateStringBuilder.append(
				(predicateStringBuilder.length() == 0 ? "" : PREDICATE_DELIMITER) + conjunction.getPredicate());
		}
	}

	private String buildArrayString() {
		if (conjunctions.size() == 1 && conjunctions.get(0).isComposite()) {
			return conjunctions.get(0).getArray();
		}
		final boolean shouldPredicateBeBuilt = arrayStringBuilder.length() != 0 && predicateStringBuilder.length() != 0;
		if (shouldPredicateBeBuilt) {
			String distanceAdjusted = "e";
			if (!queryCreator.getGeoFields().isEmpty()) {
				final String geoFields = String.format("e.%s[0], e.%s[1]", queryCreator.getGeoFields().get(0),
					queryCreator.getGeoFields().get(0));
				distanceAdjusted = String.format("MERGE(e, {'_distance': DISTANCE(%s, %%f, %%f)})", geoFields);
			}
			final String array = String.format(SUBQUERY_TEMPLATE, queryCreator.getCollectionName(),
				predicateStringBuilder.toString(), distanceAdjusted);
			arrayStringBuilder.append((arrayStringBuilder.length() == 0 ? "" : ARRAY_DELIMITER) + array);
		}
		if (arrays > 1 || shouldPredicateBeBuilt) {
			return "UNION(" + arrayStringBuilder.toString() + ")";
		}
		return arrayStringBuilder.toString();
	}

	private String buildPredicateSring() {
		if (conjunctions.size() == 1 && conjunctions.get(0).isComposite()) {
			return conjunctions.get(0).getPredicate();
		}
		return arrayStringBuilder.length() == 0 ? predicateStringBuilder.toString() : "";
	}

	public Disjunction build() {
		final String arrayString = String.format(buildArrayString(), queryCreator.getUniquePoint()[0],
			queryCreator.getUniquePoint()[1]);
		return new Disjunction(arrayString, buildPredicateSring());
	}
}
