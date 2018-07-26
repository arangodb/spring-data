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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.util.TypeInformation;

import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.util.MapBuilder;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 *
 */
public class RelationsResolver extends AbstractResolver<Relations>
		implements RelationResolver<Relations>, AbstractResolver.ResolverCallback<Relations> {

	private final ArangoOperations template;

	public RelationsResolver(final ArangoOperations template) {
		super(template.getConverter().getConversionService());
		this.template = template;
	}

	@Override
	public Object resolveOne(final String id, final TypeInformation<?> type, final Relations annotation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object resolveMultiple(final String id, final TypeInformation<?> type, final Relations annotation) {
		return annotation.lazy() ? proxy(id, type, annotation, this) : resolve(id, type, annotation);
	}

	@Override
	public Object resolve(final String id, final TypeInformation<?> type, final Relations annotation) {
		final Class<?> compType = getNonNullComponentType(type).getType();

		final String query = String.format(
			"WITH @@vertex FOR v IN %d .. %d %s @start @@edges OPTIONS {bfs: true, uniqueVertices: \"global\"} RETURN v", //
			Math.max(1, annotation.minDepth()), //
			Math.max(1, annotation.maxDepth()), //
			annotation.direction());

		final String edges = Arrays.stream(annotation.edges()).map(e -> template.collection(e).name())
				.collect(Collectors.joining(","));

		final Map<String, Object> bindVars = new MapBuilder()//
				.put("start", id) //
				.put("@vertex", compType) //
				.put("@edges", edges) //
				.get();

		return template.query(query, bindVars, compType).asListRemaining();

	}

}
