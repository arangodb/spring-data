/**
 *
 */
package com.arangodb.springframework.repository.query.derived;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Mark
 *
 */
public class Criteria {

	private static final String AND_DELIMITER = " AND ";
	private static final String OR_DELIMITER = " OR ";

	private final StringBuilder predicate;
	private final Set<Class<?>> with;

	public Criteria() {
		this("", new HashSet<>());
	}

	public Criteria(final String predicate, final Set<Class<?>> with) {
		this.predicate = new StringBuilder(predicate);
		this.with = with;
	}

	public String getPredicate() {
		return predicate.toString();
	}

	public Collection<Class<?>> getWith() {
		return with;
	}

	public Criteria and(final Criteria criteria) {
		return add(AND_DELIMITER, criteria);
	}

	public Criteria or(final Criteria criteria) {
		return add(OR_DELIMITER, criteria);
	}

	private Criteria add(final String delimiter, final Criteria criteria) {
		if (criteria == null) {
			return this;
		}
		if (predicate.length() > 0) {
			predicate.append(delimiter);
		}
		predicate.append(criteria.predicate);
		with.addAll(criteria.with);
		return this;
	}

}
