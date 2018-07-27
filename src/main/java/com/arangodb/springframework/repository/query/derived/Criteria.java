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

	public static Criteria eql(final String property, final int index) {
		return new Criteria(property + " == @" + index);
	}

	public static Criteria neql(final String property, final int index) {
		return new Criteria(property + " != @" + index);
	}

	public static Criteria isTrue(final String property) {
		return new Criteria(property + " == true");
	}

	public static Criteria isFalse(final String property) {
		return new Criteria(property + " == false");
	}

	public static Criteria isNull(final String property) {
		return new Criteria(property + " == null");
	}

	public static Criteria isNotNull(final String property) {
		return new Criteria(property + " != null");
	}

	public static Criteria exists(final String document, final String attribute) {
		return new Criteria("HAS(" + document + ", '" + attribute + "')");
	}

	public static Criteria lt(final String property, final int index) {
		return new Criteria(property + " < @" + index);
	}

	public static Criteria gt(final String property, final int index) {
		return new Criteria(property + " > @" + index);
	}

	public static Criteria lte(final String property, final int index) {
		return new Criteria(property + " <= @" + index);
	}

	public static Criteria gte(final String property, final int index) {
		return new Criteria(property + " >= @" + index);
	}

	public static Criteria like(final String property, final int index) {
		return new Criteria(property + " LIKE @" + index);
	}

	public static Criteria notLike(final String property, final int index) {
		return new Criteria("NOT(" + property + " LIKE @" + index + ")");
	}

	public static Criteria regex(final String property, final int index, final boolean caseInsensitive) {
		return new Criteria("REGEX_TEST(" + property + ", @" + index + ", " + caseInsensitive + ")");
	}

	public static Criteria in(final String property, final int index) {
		return new Criteria(property + " IN @" + index);
	}

	public static Criteria nin(final String property, final int index) {
		return new Criteria(property + " NOT IN @" + index);
	}

	public static Criteria in(final int index, final String property) {
		return new Criteria("@" + index + " IN " + property);
	}

	public static Criteria nin(final int index, final String property) {
		return new Criteria("@" + index + " NOT IN " + property);
	}

	public static Criteria contains(final String property, final int index) {
		return new Criteria("CONTAINS(" + property + ", @" + index + ")");
	}

	public static Criteria distance(final String property, final int indexLat, final int indexLong, final int indexDist) {
		return new Criteria(
				"DISTANCE(" + property + "[0], " + property + "[1], @" + indexLat + ", @" + indexLong + ") <= @" + indexDist);
	}

}
