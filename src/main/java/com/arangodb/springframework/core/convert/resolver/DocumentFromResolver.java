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

import org.springframework.data.util.TypeInformation;

import com.arangodb.ArangoCursor;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.util.MapBuilder;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 *
 */
public class DocumentFromResolver extends AbstractResolver<From> implements RelationResolver<From> {

	private final ArangoOperations template;

	public DocumentFromResolver(final ArangoOperations template) {
		super(template.getConverter().getConversionService());
		this.template = template;
	}

	@Override
	public Object resolveOne(final String id, final TypeInformation<?> type, final From annotation) {
		return annotation.lazy() ? proxy(id, type, annotation, (i, t, a) -> _resolveOne(i, t))
				: _resolveOne(id, type);
	}

	private Object _resolveOne(final String id, final TypeInformation<?> type) {
		return _resolve(id, type.getType(), true).first();
	}

	@Override
	public Object resolveMultiple(final String id, final TypeInformation<?> type, final From annotation) {
		return annotation.lazy() ? proxy(id, type, annotation, (i, t, a) -> _resolveMultiple(i, t))
				: _resolveMultiple(id, type);
	}

	private Object _resolveMultiple(final String id, final TypeInformation<?> type) {
		return _resolve(id, getNonNullComponentType(type).getType(), false).asListRemaining();
	}

	private ArangoCursor<?> _resolve(final String id, final Class<?> type, final boolean limit) {
		final String query = String.format("FOR e IN @@edge FILTER e._from == @id %s RETURN e", limit ? "LIMIT 1" : "");
		return template.query(query, new MapBuilder().put("@edge", type).put("id", id).get(), new AqlQueryOptions(),
			type);
	}

}
