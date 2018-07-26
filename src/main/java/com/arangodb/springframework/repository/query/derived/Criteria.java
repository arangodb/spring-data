/**
 *
 */
package com.arangodb.springframework.repository.query.derived;

/**
 * @author Mark
 *
 */
public class Criteria {

	private static final String AND_DELIMITER = " AND ";
	private static final String OR_DELIMITER = " OR ";

	private final StringBuilder predicate;

	public Criteria() {
		this("");
	}

	public Criteria(final String predicate) {
		this.predicate = new StringBuilder(predicate);
	}

	public String getPredicate() {
		return predicate.toString();
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
		return this;
	}

	public static Criteria eql(final String path, final int index) {
		return new Criteria(path + " == @" + index);
	}

}
