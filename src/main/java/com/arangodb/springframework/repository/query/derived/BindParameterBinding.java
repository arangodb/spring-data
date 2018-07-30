/**
 *
 */
package com.arangodb.springframework.repository.query.derived;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Range;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;

import com.arangodb.springframework.repository.query.derived.geo.Ring;

/**
 * @author Mark
 *
 */
public class BindParameterBinding {

	interface UniqueCheck {
		void check(Point point);
	}

	private final Map<String, Object> bindVars;

	public BindParameterBinding(final Map<String, Object> bindVars) {
		super();
		this.bindVars = bindVars;
	}

	public int bind(
		final Object value,
		final boolean shouldIgnoreCase,
		final Boolean borderStatus,
		final UniqueCheck uniqueCheck,
		final int startIndex) {
		int index = startIndex;
		final Object caseAdjusted = ignoreArgumentCase(value, shouldIgnoreCase);
		final Class<? extends Object> clazz = caseAdjusted.getClass();
		if (clazz == Distance.class) {
			final Distance distance = (Distance) caseAdjusted;
			bind(index++, convertDistanceToMeters(distance));
		} else if (borderStatus != null && borderStatus) {
			bind(index++, escapeSpecialCharacters((String) caseAdjusted) + "%");
		} else if (borderStatus != null) {
			bind(index++, "%" + escapeSpecialCharacters((String) caseAdjusted));
		} else {
			bind(index++, caseAdjusted);
		}
		return index;
	}

	public int bindPolygon(final Object value, final boolean shouldIgnoreCase, final int startIndex) {
		int index = startIndex;
		final Polygon polygon = (Polygon) ignoreArgumentCase(value, shouldIgnoreCase);
		final List<List<Double>> points = new LinkedList<>();
		polygon.forEach(p -> {
			final List<Double> point = new LinkedList<>();
			point.add(p.getY());
			point.add(p.getX());
			points.add(point);
		});
		bind(index++, points);
		return index;
	}

	public int bindRing(
		final Object value,
		final boolean shouldIgnoreCase,
		final UniqueCheck uniqueCheck,
		final int startIndex) {
		int index = startIndex;
		final Ring<?> ring = (Ring<?>) ignoreArgumentCase(value, shouldIgnoreCase);
		final Point point = ring.getPoint();
		index = bindPoint(point, uniqueCheck, index);
		final Range<?> range = ring.getRange();
		index = bindRange(range, index);
		return index;
	}

	public int bindRange(final Object value, final boolean shouldIgnoreCase, final int startIndex) {
		final int index = startIndex;
		final Range<?> range = (Range<?>) ignoreArgumentCase(value, shouldIgnoreCase);
		return bindRange(range, index);
	}

	public int bindBox(final Object value, final boolean shouldIgnoreCase, final int startIndex) {
		int index = startIndex;
		final Box box = (Box) ignoreArgumentCase(value, shouldIgnoreCase);
		final Point first = box.getFirst();
		final Point second = box.getSecond();
		final double minLatitude = Math.min(first.getY(), second.getY());
		final double maxLatitude = Math.max(first.getY(), second.getY());
		final double minLongitude = Math.min(first.getX(), second.getX());
		final double maxLongitude = Math.max(first.getX(), second.getX());
		bind(index++, minLatitude);
		bind(index++, maxLatitude);
		bind(index++, minLongitude);
		bind(index++, maxLongitude);
		return index;
	}

	public int bindPoint(
		final Object value,
		final boolean shouldIgnoreCase,
		final UniqueCheck uniqueCheck,
		final int startIndex) {
		return bindPoint((Point) ignoreArgumentCase(value, shouldIgnoreCase), uniqueCheck, startIndex);
	}

	private int bindPoint(final Point point, final UniqueCheck uniqueCheck, final int startIndex) {
		uniqueCheck.check(point);
		int index = startIndex;
		bind(index++, point.getY());
		bind(index++, point.getX());
		return index;
	}

	public int bindCircle(
		final Object value,
		final boolean shouldIgnoreCase,
		final UniqueCheck uniqueCheck,
		final int startIndex) {
		int index = startIndex;
		final Circle circle = (Circle) ignoreArgumentCase(value, shouldIgnoreCase);
		final Point center = circle.getCenter();
		uniqueCheck.check(center);
		bind(index++, center.getY());
		bind(index++, center.getX());
		bind(index++, convertDistanceToMeters(circle.getRadius()));
		return index;
	}

	private int bindRange(final Range<?> range, int index) {
		Object lowerBound = range.getLowerBound().getValue().get();
		Object upperBound = range.getUpperBound().getValue().get();
		if (lowerBound.getClass() == Distance.class && upperBound.getClass() == lowerBound.getClass()) {
			lowerBound = convertDistanceToMeters((Distance) lowerBound);
			upperBound = convertDistanceToMeters((Distance) upperBound);
		}
		bind(index++, lowerBound);
		bind(index++, upperBound);
		return index;
	}

	private void bind(final int index, final Object value) {
		bindVars.put(Integer.toString(index), value);
	}

	private double convertDistanceToMeters(final Distance distance) {
		return distance.getNormalizedValue() * Metrics.KILOMETERS.getMultiplier() * 1000;
	}

	/**
	 * Escapes special characters which could be used in an operand of LIKE operator
	 *
	 * @param string
	 * @return
	 */
	private String escapeSpecialCharacters(final String string) {
		final StringBuilder escaped = new StringBuilder();
		for (final char character : string.toCharArray()) {
			if (character == '%' || character == '_' || character == '\\') {
				escaped.append('\\');
			}
			escaped.append(character);
		}
		return escaped.toString();
	}

	/**
	 * Lowers case of a given argument if its type is String, Iterable<String> or String[] if shouldIgnoreCase is true
	 *
	 * @param argument
	 * @param shouldIgnoreCase
	 * @return
	 */
	private Object ignoreArgumentCase(final Object argument, final boolean shouldIgnoreCase) {
		if (!shouldIgnoreCase) {
			return argument;
		}
		if (argument instanceof String) {
			return ((String) argument).toLowerCase();
		}
		final List<String> lowered = new LinkedList<>();
		if (argument.getClass().isArray()) {
			final String[] array = (String[]) argument;
			for (final String string : array) {
				lowered.add(string.toLowerCase());
			}
		} else {
			@SuppressWarnings("unchecked")
			final Iterable<String> iterable = (Iterable<String>) argument;
			for (final Object object : iterable) {
				lowered.add(((String) object).toLowerCase());
			}
		}
		return lowered;
	}

}
