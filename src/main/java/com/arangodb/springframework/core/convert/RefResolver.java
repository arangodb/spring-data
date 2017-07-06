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

package com.arangodb.springframework.core.convert;

import java.util.ArrayList;
import java.util.Collection;

import com.arangodb.springframework.core.ArangoOperations;

/**
 * @author Mark Vollmary
 *
 */
public class RefResolver implements ReferenceResolver {

	private final ArangoOperations template;

	public RefResolver(final ArangoOperations template) {
		super();
		this.template = template;
	}

	@Override
	public <T> T resolve(final String id, final Class<T> type) {
		return template.getDocument(id, type);
	}

	@Override
	public <T> Iterable<T> resolve(final Collection<String> ids, final Class<T> type) {
		final Collection<T> docs = new ArrayList<>();
		for (final String id : ids) {
			docs.add(template.getDocument(id, type));
		}
		return docs;
	}

}
