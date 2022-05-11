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

import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.core.geo.GeoJson;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import com.arangodb.springframework.core.util.AqlUtils;
import com.arangodb.springframework.repository.query.ArangoParameterAccessor;
import com.arangodb.springframework.repository.query.derived.geo.Ring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.Pair;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates a full AQL query from a PartTree and ArangoParameterAccessor
 */
public class DerivedQueryCreator extends AbstractQueryCreator<Pair<String, ? extends Collection<String>>, Criteria> {

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

	private final ArangoMappingContext context;
	private final Class<?> domainClass;
	private final String collectionName;
	private final PartTree tree;
	private final ArangoParameterAccessor accessor;
	private final List<String> geoFields;
	private final Set<String> withCollections;
	private final BindParameterBinding binding;

	// whether any query field type is a type encoded as geoJson, only considered if isUnique == true
	private boolean hasGeoJsonType  = false;
	private Point uniquePoint = null;
	private String uniqueLocation = null;
	private Boolean isUnique = null;
	private int bindingCounter = 0;

	public DerivedQueryCreator(
		final ArangoMappingContext context,
		final Class<?> domainClass, final PartTree tree, final ArangoParameterAccessor accessor,
		final BindParameterBinding binder, final List<String> geoFields) {
		super(tree, accessor);
		this.context = context;
		this.domainClass = domainClass;
		collectionName = AqlUtils.buildCollectionName(context.getPersistentEntity(domainClass).getCollection());
		this.tree = tree;
		this.accessor = accessor;
		this.geoFields = geoFields;
		this.binding = binder;
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
	 * Builds a full AQL query from a built Disjunction, additional information from PartTree and special parameters
	 * caught by ArangoParameterAccessor
	 *
	 * @param criteria
	 * @param sort
	 * @return
	 */
	@Override
	protected Pair<String, ? extends Collection<String>> complete(final Criteria criteria, final Sort sort) {
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

		String sortString = " " + AqlUtils.buildSortClause(AqlUtils.toPersistentSort(sort, context, domainClass), "e");
		if ((!this.geoFields.isEmpty() || isUnique != null && isUnique) && !tree.isDelete() && !tree.isCountProjection()
				&& !tree.isExistsProjection()) {

			String distanceSortKey = " SORT ";
			if (hasGeoJsonType) {
				distanceSortKey += Criteria
						.geoDistance(uniqueLocation, bind(getUniqueGeoJsonPoint())).getPredicate();
			} else {
				distanceSortKey += Criteria
						.distance(uniqueLocation, bind(getUniquePoint()[0]), bind(getUniquePoint()[1])).getPredicate();
			}
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
			query.append(" ").append(AqlUtils.buildLimitClause(pageable));
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
				if (hasGeoJsonType) {
					query.append(format("MERGE(e, { '_distance': %s })",
							Criteria.geoDistance(uniqueLocation, bind(getUniqueGeoJsonPoint())).getPredicate()));
				} else {
					query.append(format("MERGE(e, { '_distance': %s })",
							Criteria.distance(uniqueLocation, bind(getUniquePoint()[0]), bind(getUniquePoint()[1]))
									.getPredicate()));
				}
			}
		}
		return Pair.of(query.toString(), withCollections);
	}

	public double[] getUniquePoint() {
		if (uniquePoint == null) {
			return new double[2];
		}
		return new double[] { uniquePoint.getY(), uniquePoint.getX() };
	}

	public Point getUniqueGeoJsonPoint() {
		if (uniquePoint == null) {
			return new Point(0,0);
		}
		return uniquePoint;
	}

	private String ignorePropertyCase(final Part part) {
		final String property = getProperty(part);
		return ignorePropertyCase(part, property);
	}

	/**
	 * Wrapps property expression in order to lower case. Only properties of type String or Iterable<String> are lowered
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
		return "e." + context.getPersistentPropertyPath(part.getProperty()).toPath(".",
				p -> AqlUtils.buildFieldName(p.getFieldName()));
	}

	/**
	 * Creates a predicate template with one String placeholder for a Part-specific predicate expression from properties
	 * in PropertyPath which represent references or collections, and, also, returns a 2nd String representing property
	 * to be used in a Part-specific predicate expression
	 *
	 * @param part
	 * @return
	 */
	private String[] createPredicateTemplateAndPropertyString(final Part part) {
		int varsUsed = 0;
		final String PREDICATE_TEMPLATE = "(%s FILTER %%s RETURN 1)[0] == 1";
		final PersistentPropertyPath<?> persistentPropertyPath = context.getPersistentPropertyPath(part.getProperty());
		StringBuilder simpleProperties = new StringBuilder();
		String predicateTemplate = "";
		int propertiesLeft = persistentPropertyPath.getLength();
		for (final Object object : persistentPropertyPath) {
			--propertiesLeft;
			final ArangoPersistentProperty property = (ArangoPersistentProperty) object;
			if (propertiesLeft == 0) {
				simpleProperties.append("." + AqlUtils.buildFieldName(property.getFieldName()));
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
					final String name = simpleProperties.toString() + "." + AqlUtils.buildFieldName(property.getFieldName());
					simpleProperties = new StringBuilder();
					final String iteration = format(TEMPLATE, entity, collection, entity, prevEntity, name);
					final String predicate = format(PREDICATE_TEMPLATE, iteration);
					predicateTemplate = predicateTemplate.length() == 0 ? predicate
							: format(predicateTemplate, predicate);
				} else {
					// collection
					final String TEMPLATE = "FOR %s IN TO_ARRAY(%s%s)";
					final String prevEntity = "e" + (varsUsed == 0 ? "" : Integer.toString(varsUsed));
					final String entity = "e" + Integer.toString(++varsUsed);
					final String name = simpleProperties.toString() + "." + AqlUtils.buildFieldName(property.getFieldName());
					simpleProperties = new StringBuilder();
					final String iteration = format(TEMPLATE, entity, prevEntity, name);
					final String predicate = format(PREDICATE_TEMPLATE, iteration);
					predicateTemplate = predicateTemplate.length() == 0 ? predicate
							: format(predicateTemplate, predicate);
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
					final String name = simpleProperties.toString() + "." + AqlUtils.buildFieldName(property.getFieldName());
					simpleProperties = new StringBuilder();
					final String iteration = format(TEMPLATE, entity, collection, entity, prevEntity, name);
					final String predicate = format(PREDICATE_TEMPLATE, iteration);
					predicateTemplate = predicateTemplate.length() == 0 ? predicate
							: format(predicateTemplate, predicate);
				} else {
					// simple property
					simpleProperties.append("." + AqlUtils.buildFieldName(property.getFieldName()));
				}
			}
		}
		return new String[] { predicateTemplate,
				"e" + (varsUsed == 0 ? "" : Integer.toString(varsUsed)) + simpleProperties.toString() };
	}

	/**
	 * Determines whether the case for a Part should be ignored based on property type and IgnoreCase keywords in the
	 * method name
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
	 * Ensures that Points used in geospatial parts of non-nested properties are the same in case geospatial return type
	 * is expected
	 *
	 * @param point
	 */
	private void checkUniquePoint(final Point point) {
		final boolean isStillUnique = (uniquePoint == null || uniquePoint.equals(point));
		if (!isStillUnique) {
			isUnique = false;
		}
		if (!geoFields.isEmpty()) {
			Assert.isTrue(uniquePoint == null || uniquePoint.equals(point),
				"Different Points are used - Distance is ambiguous");
		}
		uniquePoint = point;
	}

	/**
	 * Ensures that the same geo fields are used in geospatial parts of non-nested properties are the same in case
	 * geospatial return type is expected
	 *
	 * @param part
	 */
	private void checkUniqueLocation(final Part part) {
		isUnique = isUnique == null ? true : isUnique;
		isUnique = (uniqueLocation == null || uniqueLocation.equals(ignorePropertyCase(part))) ? isUnique : false;
		if (!geoFields.isEmpty()) {
			Assert.isTrue(isUnique, "Different location fields are used - Distance is ambiguous");
		}
		uniqueLocation = ignorePropertyCase(part);
	}

	private Criteria createCriteria(final Part part, final Iterator<Object> iterator) {
		collectWithCollections(part.getProperty());
		final String[] templateAndProperty = createPredicateTemplateAndPropertyString(part);
		final String template = templateAndProperty[0];
		final String property = templateAndProperty[1];
		Criteria criteria = null;
		final boolean checkUnique = part.getProperty().toDotPath().split(".").length <= 1;
		Class<?> type = part.getProperty().getType();

		// whether the current field type is a type encoded as geoJson
		final boolean isGeoJsonType = Point.class.isAssignableFrom(type) ||
				Polygon.class.isAssignableFrom(type) ||
				GeoJson.class.isAssignableFrom(type);
		if (isGeoJsonType) {
			hasGeoJsonType = true;
		}

		switch (part.getType()) {
		case SIMPLE_PROPERTY:
			criteria = Criteria.eql(ignorePropertyCase(part, property), bind(part, iterator));
			break;
		case NEGATING_SIMPLE_PROPERTY:
			criteria = Criteria.neql(ignorePropertyCase(part, property), bind(part, iterator));
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
			criteria = Criteria.lt(ignorePropertyCase(part, property), bind(part, iterator));
			break;
		case AFTER:
		case GREATER_THAN:
			criteria = Criteria.gt(ignorePropertyCase(part, property), bind(part, iterator));
			break;
		case LESS_THAN_EQUAL:
			criteria = Criteria.lte(ignorePropertyCase(part, property), bind(part, iterator));
			break;
		case GREATER_THAN_EQUAL:
			criteria = Criteria.gte(ignorePropertyCase(part, property), bind(part, iterator));
			break;
		case BETWEEN:
			criteria = Criteria.gte(ignorePropertyCase(part, property), bind(part, iterator))
					.and(Criteria.lte(ignorePropertyCase(part, property), bind(part, iterator)));
			break;
		case LIKE:
			criteria = Criteria.like(ignorePropertyCase(part, property), bind(part, iterator));
			break;
		case NOT_LIKE:
			criteria = Criteria.notLike(ignorePropertyCase(part, property), bind(part, iterator));
			break;
		case STARTING_WITH:
			criteria = Criteria.like(ignorePropertyCase(part, property), bind(part, iterator, true));
			break;
		case ENDING_WITH:
			criteria = Criteria.like(ignorePropertyCase(part, property), bind(part, iterator, false));
			break;
		case REGEX:
			criteria = Criteria.regex(ignorePropertyCase(part, property), bind(part, iterator), shouldIgnoreCase(part));
			break;
		case IN:
			criteria = Criteria.in(ignorePropertyCase(part, property), bind(part, iterator));
			break;
		case NOT_IN:
			criteria = Criteria.nin(ignorePropertyCase(part, property), bind(part, iterator));
			break;
		case CONTAINING:
			if (part.getProperty().getTypeInformation().isCollectionLike()) {
				criteria = Criteria.in(bind(part, iterator), ignorePropertyCase(part, property));
			} else {
				criteria = Criteria.contains(ignorePropertyCase(part, property), bind(part, iterator));
			}
			break;
		case NOT_CONTAINING:
			criteria = Criteria.nin(bind(part, iterator), ignorePropertyCase(part, property));
			break;
		case NEAR:
			if (checkUnique) {
				checkUniqueLocation(part);
			}
			Assert.isTrue(iterator.hasNext(), "Too few arguments passed");
			final Object nearValue = iterator.next();
			if (nearValue instanceof Point point) {
				checkUniquePoint(point);
			} else {
				bindingCounter = binding.bind(nearValue, shouldIgnoreCase(part), null, point -> checkUniquePoint(point),
						bindingCounter);
			}
			criteria = null;
			break;
		case WITHIN:
			if (checkUnique) {
				checkUniqueLocation(part);
			}
			final int index = bindingCounter;
			for (int i = 0; iterator.hasNext() && i < 2; i++) {
				final Object value = iterator.next();
				if (value instanceof Range || value instanceof Ring) {
					if (checkUnique) {
						checkUniqueLocation(part);
					}
					if (isGeoJsonType) {
						criteria = Criteria
								.lte(index + 1,
										Criteria.geoDistance(ignorePropertyCase(part, property), index).getPredicate())
								.and(Criteria.lte(
										Criteria.geoDistance(ignorePropertyCase(part, property), index).getPredicate(),
										index + 2));
					} else {
						criteria = Criteria
								.lte(index + 2,
										Criteria.distance(ignorePropertyCase(part, property), index, index + 1).getPredicate())
								.and(Criteria.lte(
										Criteria.distance(ignorePropertyCase(part, property), index, index + 1).getPredicate(),
										index + 3));
					}
					if (value instanceof Range) {
						bindRange(part, value);
					} else {
						bindRing(part, value, isGeoJsonType);
					}
					break;
				} else if (value instanceof Box) {
					if (isGeoJsonType) {
						criteria = Criteria.geoContains(bind(part, value, null), ignorePropertyCase(part, property));
					} else {
						criteria = Criteria.lte(index, ignorePropertyCase(part, property) + "[0]")
								.and(Criteria.lte(ignorePropertyCase(part, property) + "[0]", index + 1))
								.and(Criteria.lte(index + 2, ignorePropertyCase(part, property) + "[1]"))
								.and(Criteria.lte(ignorePropertyCase(part, property) + "[1]", index + 3));
						bindBox(part, value);
					}
					break;
                } else if (value instanceof Polygon) {
                    if (isGeoJsonType) {
						criteria = Criteria.geoContains(bind(part, value, null), ignorePropertyCase(part, property));
                    } else {
                        criteria = Criteria.isInPolygon(bindPolygon(part, value), ignorePropertyCase(part, property));
                    }
                    break;
                } else {
					if (value instanceof Circle) {
						bindCircle(part, value, isGeoJsonType);
						break;
					} else if (value instanceof Point) {
						bindPoint(part, value, isGeoJsonType);
					} else {
						bind(part, value, null);
					}
				}
			}
			if (criteria == null) {
				if(isGeoJsonType) {
					criteria = Criteria.lte(
							Criteria.geoDistance(ignorePropertyCase(part, property), index).getPredicate(), index + 1);
				} else {
					criteria = Criteria.lte(
							Criteria.distance(ignorePropertyCase(part, property), index, index + 1).getPredicate(), index + 2);
				}
			}
			break;
		default:
			throw new IllegalArgumentException(format("Part.Type \"%s\" not supported", part.getType().toString()));
		}
		if (!geoFields.isEmpty()) {
			Assert.isTrue(isUnique == null || isUnique, "Distance is ambiguous for multiple locations");
		}
		return template.isEmpty() ? criteria : new Criteria(format(template, criteria.getPredicate()));
	}

	private int bind(final Part part, final Iterator<Object> iterator) {
		return bind(part, iterator, null);
	}

	private int bind(final Part part, final Iterator<Object> iterator, final Boolean borderStatus) {
		Assert.isTrue(iterator.hasNext(), "Too few arguments passed");
		return bind(part, iterator.next(), borderStatus);
	}

	private int bind(final Part part, final Object value, final Boolean borderStatus) {
		final int index = bindingCounter;
		bindingCounter = binding.bind(value, shouldIgnoreCase(part), borderStatus, point -> checkUniquePoint(point),
			bindingCounter);
		return index;
	}

	private int bind(final Object value) {
		final int index = bindingCounter;
		bindingCounter = binding.bind(value, false, null, point -> checkUniquePoint(point), bindingCounter);
		return index;
	}

	private void bindPoint(final Part part, final Object value, final boolean toGeoJson) {
		bindingCounter = binding.bindPoint(value, shouldIgnoreCase(part), point -> checkUniquePoint(point),
			bindingCounter, toGeoJson);
	}

	private void bindCircle(final Part part, final Object value, final boolean toGeoJson) {
		bindingCounter = binding.bindCircle(value, shouldIgnoreCase(part), point -> checkUniquePoint(point),
			bindingCounter, toGeoJson);
	}

	private void bindRange(final Part part, final Object value) {
		bindingCounter = binding.bindRange(value, shouldIgnoreCase(part), bindingCounter);
	}

	private void bindRing(final Part part, final Object value, final boolean toGeoJson) {
		bindingCounter = binding.bindRing(value, shouldIgnoreCase(part), point -> checkUniquePoint(point),
			bindingCounter, toGeoJson);
	}

	private void bindBox(final Part part, final Object value) {
		bindingCounter = binding.bindBox(value, shouldIgnoreCase(part), bindingCounter);
	}

	private int bindPolygon(final Part part, final Object value) {
		final int index = bindingCounter;
		bindingCounter = binding.bindPolygon(value, shouldIgnoreCase(part), bindingCounter);
		return index;
	}

	private void collectWithCollections(final PropertyPath propertyPath) {
		propertyPath.stream()
				.filter(property -> {
					ArangoPersistentProperty p = context.getPersistentPropertyPath(property).getBaseProperty();
					if (p == null) return false;
					Optional<Ref> ref = p.getRef();
					Optional<Relations> rels = p.getRelations();
					return ref.isPresent() || rels.isPresent();
				})
				.map(property -> property.isCollection() ? property.getTypeInformation().getComponentType()
						: property.getTypeInformation())
				.filter(Objects::nonNull)
				.map(context::getPersistentEntity)
				.filter(Objects::nonNull)
				.map(entity -> AqlUtils.buildCollectionName(entity.getCollection()))
				.forEach(withCollections::add);
	}

	private String format(final String format, final Object... args) {
		return String.format(Locale.ENGLISH, format, args);
	}

}
