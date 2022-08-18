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

package com.arangodb.springframework.core.convert.resolver;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.util.TypeInformation;

import com.arangodb.ArangoCursor;
import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.util.MapBuilder;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 *
 */
public class RelationsResolver extends AbstractResolver<Relations> implements RelationResolver<Relations> {

	private final ArangoOperations template;

	public RelationsResolver(final ArangoOperations template) {
		super(template.getConverter().getConversionService());
		this.template = template;
	}

	@Override
	public Object resolveOne(final String id, final TypeInformation<?> type, final Collection<TypeInformation<?>> traversedTypes, final Relations annotation) {
		return annotation.lazy() ? proxy(id, type, annotation, (i, t, a) -> _resolveOne(i, t, traversedTypes, a))
				: _resolveOne(id, type, traversedTypes, annotation);
	}

	@Override
	public Object resolveMultiple(final String id, final TypeInformation<?> type, final Collection<TypeInformation<?>> traversedTypes, final Relations annotation) {
		return annotation.lazy() ? proxy(id, type, annotation, (i, t, a) -> _resolveMultiple(i, t, traversedTypes, a))
				: _resolveMultiple(id, type, traversedTypes, annotation);
	}

	private Object _resolveOne(final String id, final TypeInformation<?> type, final Collection<TypeInformation<?>> traversedTypes, final Relations annotation) {
		Collection<Class<?>> rawTypes = new ArrayList<>();
		for (TypeInformation<?> it : traversedTypes) {
			rawTypes.add(it.getType());
		}
		return _resolve(id, type.getType(), rawTypes, annotation, true).first();
	}

	private Object _resolveMultiple(final String id, final TypeInformation<?> type, final Collection<TypeInformation<?>> traversedTypes, final Relations annotation) {
		Collection<Class<?>> rawTypes = new ArrayList<>();
		for (TypeInformation<?> it : traversedTypes) {
			rawTypes.add(it.getType());
		}
		return _resolve(id, getNonNullComponentType(type).getType(), rawTypes, annotation, false).asListRemaining();
	}

	private ArangoCursor<?> _resolve(
		final String id,
		final Class<?> type,
		final Collection<Class<?>> traversedTypes,
		final Relations annotation,
		final boolean limit) {

		final String edges = Arrays.stream(annotation.edges()).map(e -> template.collection(e).name())
				.collect(Collectors.joining(","));

		List<Class<?>> allTraversedTypes = new ArrayList<>();
		allTraversedTypes.add(type);
		allTraversedTypes.addAll(traversedTypes);

		MapBuilder bindVars = new MapBuilder().put("start", id);
		StringBuilder withClause = new StringBuilder("WITH ");
		for (int i = 0; i < allTraversedTypes.size(); i++) {
			bindVars.put("@with" + i, allTraversedTypes.get(i));
			if (i > 0) withClause.append(", ");
			withClause.append("@@with").append(i);
		}

		final String query = String.format(
				"%s FOR v IN %d .. %d %s @start %s OPTIONS {bfs: true, uniqueVertices: \"global\"} %s RETURN v", //
				withClause, //
				Math.max(1, annotation.minDepth()), //
				Math.max(1, annotation.maxDepth()), //
				annotation.direction(), //
				edges, //
				limit ? "LIMIT 1" : "");

		return template.query(query, bindVars.get(), type);
	}

}
