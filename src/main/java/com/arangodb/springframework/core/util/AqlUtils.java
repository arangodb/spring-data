/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
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

package com.arangodb.springframework.core.util;

import java.util.StringJoiner;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 
 * @author Christian Lechner
 */
public final class AqlUtils {

	private AqlUtils() {

	}

	public static String buildLimitClause(final Pageable pageable) {
		if (pageable.isUnpaged()) {
			return "";
		}

		final StringJoiner clause = new StringJoiner(", ", "LIMIT ", "");
		clause.add(String.valueOf(pageable.getOffset()));
		clause.add(String.valueOf(pageable.getPageSize()));
		return clause.toString();
	}

	public static String buildPageableClause(final Pageable pageable) {
		return buildPageableClause(pageable, null);
	}

	public static String buildPageableClause(final Pageable pageable, @Nullable final String varName) {
		return buildPageableClause(pageable, varName, new StringBuilder()).toString();
	}

	private static StringBuilder buildPageableClause(
		final Pageable pageable,
		@Nullable final String varName,
		final StringBuilder clause) {

		if (pageable.isUnpaged()) {
			return clause;
		}

		final Sort sort = pageable.getSort();
		buildSortClause(sort, varName, clause);

		if (sort.isSorted()) {
			clause.append(' ');
		}

		clause.append("LIMIT ").append(pageable.getOffset()).append(", ").append(pageable.getPageSize());
		return clause;
	}

	public static String buildSortClause(final Sort sort) {
		return buildSortClause(sort, null);
	}

	public static String buildSortClause(final Sort sort, @Nullable final String varName) {
		return buildSortClause(sort, varName, new StringBuilder()).toString();
	}

	private static StringBuilder buildSortClause(
		final Sort sort,
		@Nullable final String varName,
		final StringBuilder clause) {

		if (sort.isUnsorted()) {
			return clause;
		}

		final String prefix = StringUtils.hasText(varName) ? escapeSortProperty(varName) : null;
		clause.append("SORT ");
		boolean first = true;

		for (final Sort.Order order : sort) {
			if (!first) {
				clause.append(", ");
			} else {
				first = false;
			}

			if (prefix != null) {
				clause.append(prefix).append('.');
			}
			final String escapedProperty = escapeSortProperty(order.getProperty());
			clause.append(escapedProperty).append(' ').append(order.getDirection());
		}
		return clause;

	}

	private static String escapeSortProperty(final String str) {
		// dots are not allowed at start/end
		if (str.charAt(0) == '.' || str.charAt(str.length() - 1) == '.') {
			throw new IllegalArgumentException("Sort properties must not begin or end with a dot!");
		}

		final StringBuilder escaped = new StringBuilder();
		escaped.append('`');

		// keep track if we are inside an escaped sequence
		boolean inEscapedSeq = false;

		for (int i = 0; i < str.length(); ++i) {
			final char currChar = str.charAt(i);
			final boolean hasNext = (i + 1) < str.length();
			final char nextChar = hasNext ? str.charAt(i + 1) : '\0';

			if (currChar == '\\') {
				// keep escaped backticks
				if (nextChar == '`') {
					escaped.append("\\`");
					++i;
				}
				// escape backslashes
				else {
					escaped.append("\\\\");
				}
			}

			// current char is an unescaped backtick
			else if (currChar == '`') {
				inEscapedSeq = !inEscapedSeq;

				final boolean isStartOrEnd = i == 0 || !hasNext;
				final boolean isNextCharDotOutsideEscapedSeq = nextChar == '.' && !inEscapedSeq;

				// unescaped backticks are only allowed at start/end of attributes
				if (!isStartOrEnd && !isNextCharDotOutsideEscapedSeq) {
					throw new IllegalArgumentException(
							"Sort properties must only contain backticks at beginning/end of attributes or when escaped.");
				}
			}

			else if (currChar == '.') {
				// the dot is part of an attribute name when inside escaped sequence
				if (inEscapedSeq) {
					// add dot without escaping
					escaped.append('.');
				}

				else {
					// properties can only contain 2+ dots in escaped sequences
					if (nextChar == '.') {
						throw new IllegalArgumentException(
								"Sort properties may not contain 2+ consecutive dots when outside a backtick escape sequence!");
					}
					// consume optional backtick
					else if (nextChar == '`') {
						inEscapedSeq = !inEscapedSeq;
						++i;
					}

					// close previous escape sequence and open new one
					escaped.append("`.`");
				}
			}

			// keep others
			else {
				escaped.append(currChar);
			}
		}

		// check for an open escape sequence
		if (inEscapedSeq) {
			throw new IllegalArgumentException(
					"A sort property contains an unclosed backtick escape sequence! The cause may be a missing backtick.");
		}

		escaped.append('`');
		return escaped.toString();
	}

	public static String buildCollectionName(final String collection) {
		return collection.contains("-") ? "`" + collection + "`" : collection;
	}

}
