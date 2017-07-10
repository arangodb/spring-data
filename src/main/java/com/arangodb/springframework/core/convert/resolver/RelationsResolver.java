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

import java.util.Collection;

import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.util.MapBuilder;

/**
 * @author Mark Vollmary
 *
 */
public class RelationsResolver extends AbstractResolver<Relations>
		implements RelationResolver<Relations>, AbstractResolver.ResolverCallback<Relations> {

	private final ArangoOperations template;

	public RelationsResolver(final ArangoOperations template) {
		super();
		this.template = template;
	}

	@Override
	public Object resolveOne(final String id, final Class<?> type, final Relations annotation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object resolveMultiple(final String id, final Class<?> type, final Relations annotation) {
		return annotation.lazy() ? proxy(id, Collection.class, annotation, (i, t, a) -> resolve(i, type, a))
				: resolve(id, type, annotation);
	}

	@Override
	public Object resolve(final String id, final Class<?> type, final Relations annotation) {
		return template.query(
			"WITH @@edge FOR v IN 1.." + Math.max(1, annotation.depth()) + " " + annotation.direction()
					+ " @start @@edge OPTIONS {bfs: true, uniqueVertices: \"global\"} RETURN v",
			new MapBuilder().put("start", id).put("@edge", annotation.edge()).get(), new AqlQueryOptions(), type)
				.asListRemaining();
	}

}
