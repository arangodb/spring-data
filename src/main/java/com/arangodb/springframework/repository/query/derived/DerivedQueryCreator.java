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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.Assert;

import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import com.arangodb.springframework.core.util.AqlUtils;
import com.arangodb.springframework.repository.query.ArangoParameterAccessor;
import com.arangodb.springframework.repository.query.derived.geo.Ring;

/**
 * Creates a full AQL query from a PartTree and ArangoParameterAccessor
 */
public class DerivedQueryCreator extends AbstractQueryCreator<String, Criteria> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DerivedQueryCreator.class);
	private static final Set<Part.Type> UNSUPPORTED_IGNORE_CASE = new HashSet<>();

	static {
		UNSUPPORTED_IGNORE_CASE.add(Part.Type.EXISTS);
		UNSUPPORTED_IGNORE_CASE.add(Part.Type.TRUE);
		UNSUPPORTED_IGNORE_CASE.add(Part.Type.FALSE);
		UNSUPPORTED_IGNORE_CASE.add(Part.Type.IS_NULL);
		UNSUPPORTED_IGNORE_CASE.add(Part.Type.IS_NOT_NULL);
		UNSUPPORTED_IGNORE_CASE.add(Part.Type.NEAR);
		UNSUPPORTED_IGNORE_CASE.add(Part.Type.WITHIN);
	}

	private final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context;
	private final String collectionName;
	private final PartTree tree;
	private final Map<String, Object> bindVars;
	private final ArangoParameterAccessor accessor;
	private final List<String> geoFields;
	private final Set<String> withCollections;

	private Point uniquePoint = null;
	private String uniqueLocation = null;
	private Boolean isUnique = null;
	private int bindingCounter = 0;
	private int varsUsed = 0;
	private boolean checkUnique = false;

	public DerivedQueryCreator(
			final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context,
			final Class<?> domainClass, final PartTree tree, final ArangoParameterAccessor accessor,
			final Map<String, Object> bindVars, final List<String> geoFields) {
		super(tree, accessor);
		this.context = context;
		collectionName = collectionName(context.getPersistentEntity(domainClass).getCollection());
		this.tree = tree;
		this.bindVars = bindVars;
		this.accessor = accessor;
		this.geoFields = geoFields;
		withCollections = new HashSet<>();
	}

	@Override
	protected Criteria create(final Part part, final Iterator<Object> iterator) {
		return and(part, new Criteria(), iterator);
	}

	@Override
	protected Criteria and(final Part part, final Criteria base, final Iterator<Object> iterator) {
		return base.and(createCriteria(part, iterator));
	}

	@Override
	protected Criteria or(final Criteria base, final Criteria criteria) {
		return base.or(criteria);
	}

	/**
	 * Builds a full AQL query from a built Disjunction, additional information from
	 * PartTree and special parameters caught by ArangoParameterAccessor
	 *
	 * @param criteria
	 * @param sort
	 * @return
	 */
	@Override
	protected String complete(final Criteria criteria, final Sort sort) {
		if (tree.isDistinct() && !tree.isCountProjection()) {
			LOGGER.debug("Use of 'Distinct' is meaningful only in count queries");
		}
		final StringBuilder query = new StringBuilder();

		final String with = withCollections.stream().collect(Collectors.joining(", "));
		if (!with.isEmpty()) {
			query.append("WITH ").append(with).append(" ");
		}

		query.append("FOR ").append("e").append(" IN ").append(collectionName);

		if (!criteria.getPredicate().isEmpty()) {
			query.append(" FILTER ").append(criteria.getPredicate());
		}

		if (tree.isCountProjection() || tree.isExistsProjection()) {
			if (tree.isDistinct()) {
				query.append(" COLLECT entity = ").append("e");
			}
			query.append(" COLLECT WITH COUNT INTO length");
		}

		final String geoFields = format("%s[0], %s[1]", uniqueLocation, uniqueLocation);

		String sortString = " " + AqlUtils.buildSortClause(sort, "e");
		if ((!this.geoFields.isEmpty() || isUnique != null && isUnique) && !tree.isDelete() && !tree.isCountProjection()
				&& !tree.isExistsProjection()) {
			final String distanceSortKey = format(" SORT distance(%s, %f, %f)", geoFields, getUniquePoint()[0],
					getUniquePoint()[1]);
			if (sort.isUnsorted()) {
				sortString = distanceSortKey;
			} else {
				sortString = distanceSortKey + ", " + sortString.substring(5, sortString.length());
			}
		}
		query.append(sortString);

		if (tree.isLimiting()) {
			query.append(" LIMIT ").append(tree.getMaxResults());
		}

		final Pageable pageable = accessor.getPageable();
		if (pageable != null && pageable.isPaged()) {
			query.append(" LIMIT ").append(pageable.getOffset()).append(", ").append(pageable.getPageSize());
		}
		if (tree.isDelete()) {
			query.append(" REMOVE e IN ").append(collectionName);
		} else if (tree.isCountProjection() || tree.isExistsProjection()) {
			query.append(" RETURN length");
		} else {
			query.append(" RETURN ");
			if (this.geoFields.isEmpty()) {
				query.append("e");
			} else {
				query.append(format("MERGE(e, { '_distance': distance(%s, %f, %f) })", geoFields, getUniquePoint()[0],
						getUniquePoint()[1]));
			}
		}

		return query.toString();
	}

	private static String collectionName(final String collection) {
		return collection.contains("-") ? "`" + collection + "`" : collection;
	}

	public double[] getUniquePoint() {
		if (uniquePoint == null) {
			return new double[2];
		}
		return new double[] { uniquePoint.getY(), uniquePoint.getX() };
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

	private String ignorePropertyCase(final Part part) {
		final String property = getProperty(part);
		return ignorePropertyCase(part, property);
	}

	/**
	 * Wrapps property expression in order to lower case. Only properties of type
	 * String or Iterable<String> are lowered
	 *
	 * @param part
	 * @param property
	 * @return
	 */
	private String ignorePropertyCase(final Part part, final String property) {
		if (!shouldIgnoreCase(part)) {
			return property;
		}
		if (!part.getProperty().getLeafProperty().isCollection()) {
			return "LOWER(" + property + ")";
		}
		return format("(FOR i IN TO_ARRAY(%s) RETURN LOWER(i))", property);
	}

	/**
	 * Returns a String representing a full propertyPath e.g. "e.product.name"
	 *
	 * @param part
	 * @return
	 */
	private String getProperty(final Part part) {
		return "e."
				+ context.getPersistentPropertyPath(part.getProperty()).toPath(".", ArangoPersistentProperty::getFieldName);
	}

	/**
	 * Creates a predicate template with one String placeholder for a Part-specific
	 * predicate expression from properties in PropertyPath which represent
	 * references or collections, and, also, returns a 2nd String representing
	 * property to be used in a Part-specific predicate expression
	 *
	 * @param part
	 * @return
	 */
	private String[] createPredicateTemplateAndPropertyString(final Part part) {
		final String PREDICATE_TEMPLATE = "(%s FILTER %%s RETURN 1)[0] == 1";
		final PersistentPropertyPath<?> persistentPropertyPath = context.getPersistentPropertyPath(part.getProperty());
		StringBuilder simpleProperties = new StringBuilder();
		String predicateTemplate = "";
		int propertiesLeft = persistentPropertyPath.getLength();
		for (final Object object : persistentPropertyPath) {
			--propertiesLeft;
			final ArangoPersistentProperty property = (ArangoPersistentProperty) object;
			if (propertiesLeft == 0) {
				simpleProperties.append("." + property.getFieldName());
				break;
			}
			if (property.getRelations().isPresent()) {
				// graph traversal
				final String TEMPLATE = "FOR %s IN %s %s %s%s._id %s";
				final String nested = simpleProperties.toString();
				final Relations relations = property.getRelations().get();
				final String direction = relations.direction().name();
				final String depths = format("%s..%d", relations.minDepth(), relations.maxDepth());
				final Class<?>[] edgeClasses = relations.edges();
				final StringBuilder edgesBuilder = new StringBuilder();
				for (final Class<?> edge : edgeClasses) {
					String collection = context.getPersistentEntity(edge).getCollection();
					if (collection.split("-").length > 1) {
						collection = "`" + collection + "`";
					}
					edgesBuilder.append((edgesBuilder.length() == 0 ? "" : ", ") + collection);
				}
				final String prevEntity = "e" + (varsUsed == 0 ? "" : Integer.toString(varsUsed));
				final String entity = "e" + Integer.toString(++varsUsed);
				final String edges = edgesBuilder.toString();
				simpleProperties = new StringBuilder();
				final String iteration = format(TEMPLATE, entity, depths, direction, prevEntity, nested, edges);
				final String predicate = format(PREDICATE_TEMPLATE, iteration);
				predicateTemplate = predicateTemplate.length() == 0 ? predicate : format(predicateTemplate, predicate);
			} else if (property.isCollectionLike()) {
				if (property.getRef().isPresent()) {
					// collection of references
					final String TEMPLATE = "FOR %s IN %s FILTER %s._id IN %s%s";
					final String prevEntity = "e" + (varsUsed == 0 ? "" : Integer.toString(varsUsed));
					final String entity = "e" + Integer.toString(++varsUsed);
					String collection = context.getPersistentEntity(property.getComponentType()).getCollection();
					if (collection.split("-").length > 1) {
						collection = "`" + collection + "`";
					}
					final String name = simpleProperties.toString() + "." + property.getFieldName();
					simpleProperties = new StringBuilder();
					final String iteration = format(TEMPLATE, entity, collection, entity, prevEntity, name);
					final String predicate = format(PREDICATE_TEMPLATE, iteration);
					predicateTemplate = predicateTemplate.length() == 0 ? predicate : format(predicateTemplate, predicate);
				} else {
					// collection
					final String TEMPLATE = "FOR %s IN TO_ARRAY(%s%s)";
					final String prevEntity = "e" + (varsUsed == 0 ? "" : Integer.toString(varsUsed));
					final String entity = "e" + Integer.toString(++varsUsed);
					final String name = simpleProperties.toString() + "." + property.getFieldName();
					simpleProperties = new StringBuilder();
					final String iteration = format(TEMPLATE, entity, prevEntity, name);
					final String predicate = format(PREDICATE_TEMPLATE, iteration);
					predicateTemplate = predicateTemplate.length() == 0 ? predicate : format(predicateTemplate, predicate);
				}
			} else {
				if (property.getRef().isPresent() || property.getFrom().isPresent() || property.getTo().isPresent()) {
					// single reference
					final String TEMPLATE = "FOR %s IN %s FILTER %s._id == %s%s";
					final String prevEntity = "e" + (varsUsed == 0 ? "" : Integer.toString(varsUsed));
					final String entity = "e" + Integer.toString(++varsUsed);
					String collection = context.getPersistentEntity(property.getType()).getCollection();
					if (collection.split("-").length > 1) {
						collection = "`" + collection + "`";
					}
					final String name = simpleProperties.toString() + "." + property.getFieldName();
					simpleProperties = new StringBuilder();
					final String iteration = format(TEMPLATE, entity, collection, entity, prevEntity, name);
					final String predicate = format(PREDICATE_TEMPLATE, iteration);
					predicateTemplate = predicateTemplate.length() == 0 ? predicate : format(predicateTemplate, predicate);
				} else {
					// simple property
					simpleProperties.append("." + property.getFieldName());
				}
			}
		}
		return new String[] { predicateTemplate,
				"e" + (varsUsed == 0 ? "" : Integer.toString(varsUsed)) + simpleProperties.toString() };
	}

	/**
	 * Lowers case of a given argument if its type is String, Iterable<String> or
	 * String[] if shouldIgnoreCase is true
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

	/**
	 * Determines whether the case for a Part should be ignored based on property
	 * type and IgnoreCase keywords in the method name
	 *
	 * @param part
	 * @return
	 */
	private boolean shouldIgnoreCase(final Part part) {
		final Class<?> propertyClass = part.getProperty().getLeafProperty().getType();
		final boolean isLowerable = String.class.isAssignableFrom(propertyClass);
		final boolean shouldIgnoreCase = part.shouldIgnoreCase() != Part.IgnoreCaseType.NEVER && isLowerable
				&& !UNSUPPORTED_IGNORE_CASE.contains(part.getType());
		if (part.shouldIgnoreCase() == Part.IgnoreCaseType.ALWAYS
				&& (!isLowerable || UNSUPPORTED_IGNORE_CASE.contains(part.getType()))) {
			LOGGER.debug("Ignoring case for \"{}\" type is meaningless", propertyClass);
		}
		return shouldIgnoreCase;
	}

	/**
	 * Puts actual arguments in bindVars Map based on Part-specific information and
	 * types of arguments.
	 *
	 * @param iterator
	 * @param shouldIgnoreCase
	 * @param arguments
	 * @param borderStatus
	 * @param ignoreBindVars
	 * @return
	 */
	private ArgumentProcessingResult bindArguments(final Iterator<Object> iterator, final boolean shouldIgnoreCase,
			final int arguments, final Boolean borderStatus, final boolean ignoreBindVars) {
		int bindings = 0;
		ArgumentProcessingResult.Type type = ArgumentProcessingResult.Type.DEFAULT;
		for (int i = 0; i < arguments; ++i) {
			Assert.isTrue(iterator.hasNext(), "Too few arguments passed");
			final Object caseAdjusted = ignoreArgumentCase(iterator.next(), shouldIgnoreCase);
			if (caseAdjusted.getClass() == Polygon.class) {
				type = ArgumentProcessingResult.Type.POLYGON;
				final Polygon polygon = (Polygon) caseAdjusted;
				final List<List<Double>> points = new LinkedList<>();
				polygon.forEach(p -> {
					final List<Double> point = new LinkedList<>();
					point.add(p.getY());
					point.add(p.getX());
					points.add(point);
				});
				bindVars.put(Integer.toString(bindingCounter + bindings++), points);
				break;
			} else if (caseAdjusted.getClass() == Ring.class) {
				type = ArgumentProcessingResult.Type.RANGE;
				final Point point = ((Ring<?>) caseAdjusted).getPoint();
				checkUniquePoint(point);
				bindVars.put(Integer.toString(bindingCounter + bindings++), point.getY());
				bindVars.put(Integer.toString(bindingCounter + bindings++), point.getX());
				final Range<?> range = ((Ring<?>) caseAdjusted).getRange();
				bindings = bindRange(range, bindings);
				break;
			} else if (caseAdjusted.getClass() == Box.class) {
				type = ArgumentProcessingResult.Type.BOX;
				final Box box = (Box) caseAdjusted;
				final Point first = box.getFirst();
				final Point second = box.getSecond();
				final double minLatitude = Math.min(first.getY(), second.getY());
				final double maxLatitude = Math.max(first.getY(), second.getY());
				final double minLongitude = Math.min(first.getX(), second.getX());
				final double maxLongitude = Math.max(first.getX(), second.getX());
				bindVars.put(Integer.toString(bindingCounter + bindings++), minLatitude);
				bindVars.put(Integer.toString(bindingCounter + bindings++), maxLatitude);
				bindVars.put(Integer.toString(bindingCounter + bindings++), minLongitude);
				bindVars.put(Integer.toString(bindingCounter + bindings++), maxLongitude);
				break;
			} else if (caseAdjusted.getClass() == Circle.class) {
				final Circle circle = (Circle) caseAdjusted;
				checkUniquePoint(circle.getCenter());
				bindVars.put(Integer.toString(bindingCounter + bindings++), circle.getCenter().getY());
				bindVars.put(Integer.toString(bindingCounter + bindings++), circle.getCenter().getX());
				bindVars.put(Integer.toString(bindingCounter + bindings++), convertDistanceToMeters(circle.getRadius()));
				break;
			} else if (caseAdjusted.getClass() == Point.class) {
				final Point point = (Point) caseAdjusted;
				checkUniquePoint(point);
				if (ignoreBindVars) {
					continue;
				}
				bindVars.put(Integer.toString(bindingCounter + bindings++), point.getY());
				bindVars.put(Integer.toString(bindingCounter + bindings++), point.getX());
			} else if (caseAdjusted.getClass() == Distance.class) {
				final Distance distance = (Distance) caseAdjusted;
				bindVars.put(Integer.toString(bindingCounter + bindings++), convertDistanceToMeters(distance));
			} else if (caseAdjusted.getClass() == Range.class) {
				type = ArgumentProcessingResult.Type.RANGE;
				final Range<?> range = (Range<?>) caseAdjusted;
				bindings = bindRange(range, bindings);
			} else if (borderStatus != null && borderStatus) {
				final String string = (String) caseAdjusted;
				bindVars.put(Integer.toString(bindingCounter + bindings++), escapeSpecialCharacters(string) + "%");
			} else if (borderStatus != null) {
				final String string = (String) caseAdjusted;
				bindVars.put(Integer.toString(bindingCounter + bindings++), "%" + escapeSpecialCharacters(string));
			} else {
				bindVars.put(Integer.toString(bindingCounter + bindings++), caseAdjusted);
			}
		}
		return new ArgumentProcessingResult(type, bindings);
	}

	/**
	 * Ensures that Points used in geospatial parts of non-nested properties are the
	 * same in case geospatial return type is expected
	 *
	 * @param point
	 */
	private void checkUniquePoint(final Point point) {
		if (!checkUnique) {
			return;
		}
		final boolean isStillUnique = (uniquePoint == null || uniquePoint.equals(point));
		if (!isStillUnique) {
			isUnique = false;
		}
		if (!geoFields.isEmpty()) {
			Assert.isTrue(uniquePoint == null || uniquePoint.equals(point),
					"Different Points are used - Distance is ambiguous");
			uniquePoint = point;
		}
	}

	private int bindRange(final Range<?> range, int bindings) {
		Object lowerBound = range.getLowerBound().getValue().get();
		Object upperBound = range.getUpperBound().getValue().get();
		if (lowerBound.getClass() == Distance.class && upperBound.getClass() == lowerBound.getClass()) {
			lowerBound = convertDistanceToMeters((Distance) lowerBound);
			upperBound = convertDistanceToMeters((Distance) upperBound);
		}
		bindVars.put(Integer.toString(bindingCounter + bindings++), lowerBound);
		bindVars.put(Integer.toString(bindingCounter + bindings++), upperBound);
		return bindings;
	}

	private double convertDistanceToMeters(final Distance distance) {
		return distance.getNormalizedValue() * Metrics.KILOMETERS.getMultiplier() * 1000;
	}

	/**
	 * Ensures that the same geo fields are used in geospatial parts of non-nested
	 * properties are the same in case geospatial return type is expected
	 *
	 * @param part
	 */
	private void checkUniqueLocation(final Part part) {
		if (!checkUnique) {
			return;
		}
		isUnique = isUnique == null ? true : isUnique;
		isUnique = (uniqueLocation == null || uniqueLocation.equals(ignorePropertyCase(part))) ? isUnique : false;
		if (!geoFields.isEmpty()) {
			Assert.isTrue(isUnique, "Different location fields are used - Distance is ambiguous");
		}
		uniqueLocation = ignorePropertyCase(part);
	}

	private Criteria createCriteria(final Part part, final Iterator<Object> iterator) {
		final String[] templateAndProperty = createPredicateTemplateAndPropertyString(part);
		final String template = templateAndProperty[0];
		final String property = templateAndProperty[1];
		String clause = null;
		Criteria criteria = null;
		Boolean borderStatus = null;
		boolean ignoreBindVars = false;
		checkUnique = part.getProperty().toDotPath().split(".").length <= 1;
		int index = bindingCounter;
		switch (part.getType()) {
		case SIMPLE_PROPERTY:
			criteria = Criteria.eql(ignorePropertyCase(part, property), index++);
			break;
		case NEGATING_SIMPLE_PROPERTY:
			criteria = Criteria.neql(ignorePropertyCase(part, property), index++);
			break;
		case TRUE:
			criteria = Criteria.isTrue(ignorePropertyCase(part, property));
			break;
		case FALSE:
			criteria = Criteria.isFalse(ignorePropertyCase(part, property));
			break;
		case IS_NULL:
			criteria = Criteria.isNull(ignorePropertyCase(part, property));
			break;
		case IS_NOT_NULL:
			criteria = Criteria.isNotNull(ignorePropertyCase(part, property));
			break;
		case EXISTS:
			final String document = property.substring(0, property.lastIndexOf("."));
			final String attribute = property.substring(property.lastIndexOf(".") + 1, property.length());
			criteria = Criteria.exists(document, attribute);
			break;
		case BEFORE:
		case LESS_THAN:
			criteria = Criteria.lt(ignorePropertyCase(part, property), index++);
			break;
		case AFTER:
		case GREATER_THAN:
			criteria = Criteria.gt(ignorePropertyCase(part, property), index++);
			break;
		case LESS_THAN_EQUAL:
			criteria = Criteria.lte(ignorePropertyCase(part, property), index++);
			break;
		case GREATER_THAN_EQUAL:
			criteria = Criteria.gte(ignorePropertyCase(part, property), index++);
			break;
		case BETWEEN:
			criteria = Criteria.gte(ignorePropertyCase(part, property), index++)
					.and(Criteria.lte(ignorePropertyCase(part, property), index++));
			break;
		case LIKE:
			criteria = Criteria.like(ignorePropertyCase(part, property), index++);
			break;
		case NOT_LIKE:
			criteria = Criteria.notLike(ignorePropertyCase(part, property), index++);
			break;
		case STARTING_WITH:
			criteria = Criteria.like(ignorePropertyCase(part, property), index++);
			borderStatus = true;
			break;
		case ENDING_WITH:
			criteria = Criteria.like(ignorePropertyCase(part, property), index++);
			borderStatus = false;
			break;
		case REGEX:
			criteria = Criteria.regex(ignorePropertyCase(part, property), index++, shouldIgnoreCase(part));
			break;
		case IN:
			criteria = Criteria.in(ignorePropertyCase(part, property), index++);
			break;
		case NOT_IN:
			criteria = Criteria.nin(ignorePropertyCase(part, property), index++);
			break;
		case CONTAINING:
			if (part.getProperty().getTypeInformation().isCollectionLike()) {
				criteria = Criteria.in(index++, ignorePropertyCase(part, property));
			} else {
				criteria = Criteria.contains(ignorePropertyCase(part, property), index++);
			}
			break;
		case NOT_CONTAINING:
			criteria = Criteria.nin(index++, ignorePropertyCase(part, property));
			break;
		case NEAR:
			checkUniqueLocation(part);
			ignoreBindVars = true;
			index++;
			break;
		case WITHIN:
			checkUniqueLocation(part);
			criteria = Criteria.distance(ignorePropertyCase(part, property), index++, index++, index);
			break;
		default:
			throw new IllegalArgumentException(format("Part.Type \"%s\" not supported", part.getType().toString()));
		}
		if (!geoFields.isEmpty()) {
			Assert.isTrue(isUnique == null || isUnique, "Distance is ambiguous for multiple locations");
		}
		final int bindings;
		final ArgumentProcessingResult result = bindArguments(iterator, shouldIgnoreCase(part), index - bindingCounter,
				borderStatus, ignoreBindVars);
		bindings = result.bindings;

		if (criteria != null) {
			clause = criteria.getPredicate();
		}

		switch (result.type) {
		case RANGE:
			checkUniqueLocation(part);
			clause = format("@%d <= distance(%s[0], %s[1], @%d, @%d) AND distance(%s[0], %s[1], @%d, @%d) <= @%d",
					bindingCounter + 2, ignorePropertyCase(part, property), ignorePropertyCase(part, property), bindingCounter,
					bindingCounter + 1, ignorePropertyCase(part, property), ignorePropertyCase(part, property), bindingCounter,
					bindingCounter + 1, bindingCounter + 3);
			break;
		case BOX:
			clause = format("@%d <= %s[0] AND %s[0] <= @%d AND @%d <= %s[1] AND %s[1] <= @%d", bindingCounter,
					ignorePropertyCase(part, property), ignorePropertyCase(part, property), bindingCounter + 1,
					bindingCounter + 2, ignorePropertyCase(part, property), ignorePropertyCase(part, property),
					bindingCounter + 3);
			break;
		case POLYGON:
			clause = format("IS_IN_POLYGON(@%d, %s[0], %s[1])", bindingCounter, ignorePropertyCase(part, property),
					ignorePropertyCase(part, property));
			break;
		case DEFAULT:
		default:
			break;
		}
		bindingCounter += bindings;

		if (!template.isEmpty()) {
			clause = format(template, clause);
		}
		if (criteria != null) {
			criteria = new Criteria(clause);
		}
		bindCollections(part.getProperty());
		return criteria != null ? criteria : clause == null ? null : new Criteria(clause);
	}

	private void bindCollections(final PropertyPath propertyPath) {
		propertyPath.stream().map(property -> {
			return property.isCollection() ? property.getTypeInformation().getComponentType() : property.getTypeInformation();
		}).map(type -> {
			return context.getPersistentEntity(type);
		}).filter(entity -> {
			return entity != null;
		}).map(entity -> {
			return collectionName(entity.getCollection());
		}).forEach(withCollections::add);
	}

	private String format(final String format, final Object... args) {
		return String.format(Locale.ENGLISH, format, args);
	}

	/**
	 * Stores how many bindings where used in a Part and if or what kind of special
	 * type clause should be created
	 */
	private static class ArgumentProcessingResult {

		private final Type type;
		private final int bindings;

		public ArgumentProcessingResult(final Type type, final int bindings) {
			this.type = type;
			this.bindings = bindings;
		}

		private enum Type {
			DEFAULT, RANGE, BOX, POLYGON
		}
	}

}
