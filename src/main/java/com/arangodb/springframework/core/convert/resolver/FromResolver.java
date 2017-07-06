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

import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.util.MapBuilder;

/**
 * @author Mark Vollmary
 *
 */
public class FromResolver implements RelationResolver<From> {

	private final ArangoOperations template;

	public FromResolver(final ArangoOperations template) {
		super();
		this.template = template;
	}

	@Override
	public <T> T resolve(final String id, final Class<T> type, final From annotation) {
		return template.getDocument(id, type);
	}

	@Override
	public <T> Iterable<T> resolveMultiple(final String id, final Class<T> type, final From annotation) {
		return template
				.query("FOR e IN @@edge FILTER e._from == @id RETURN e",
					new MapBuilder().put("@edge", type).put("id", id).get(), new AqlQueryOptions(), type)
				.asListRemaining();
	}

}
