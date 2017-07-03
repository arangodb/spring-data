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

package com.arangodb.springframework.core.convert.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.convert.ReferenceResolver;
import com.arangodb.util.MapBuilder;

/**
 * @author Mark Vollmary
 *
 */
public abstract class RefResolver implements ReferenceResolver {

	public abstract ArangoOperations template();

	@Override
	public String write(final Optional<String> id, final Object obj) {
		final ArangoOperations template = template();
		if (id.isPresent()) {
			return template.query("UPSERT { _id: '@id' } INSERT @doc UPDATE @doc IN @@coll",
				new MapBuilder().put("id", id.get()).put("doc", obj)
						.put("@coll", template.determineCollectionName(obj.getClass(), id.get())).get(),
				new AqlQueryOptions(), String.class).next();
		}
		return template.insertDocument(obj).getId();
	}

	@Override
	public <T> T read(final String id, final Class<T> type) {
		return template().getDocument(id, type);
	}

	@Override
	public <T> Iterable<T> read(final Collection<String> ids, final Class<T> type) {
		final Collection<T> docs = new ArrayList<>();
		final ArangoOperations template = template();
		for (final String id : ids) {
			docs.add(template.getDocument(id, type));
		}
		return docs;
	}

}
