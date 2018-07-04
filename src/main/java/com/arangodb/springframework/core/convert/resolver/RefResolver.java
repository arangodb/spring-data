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
import java.util.stream.Collectors;

import org.springframework.data.util.TypeInformation;

import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.core.ArangoOperations;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 *
 */
public class RefResolver extends AbstractResolver<Ref>
		implements ReferenceResolver<Ref>, AbstractResolver.ResolverCallback<Ref> {

	private final ArangoOperations template;

	public RefResolver(final ArangoOperations template) {
		super(template.getConverter().getConversionService());
		this.template = template;
	}

	@Override
	public Object resolveOne(final String id, final TypeInformation<?> type, final Ref annotation) {
		return annotation.lazy() ? proxy(id, type, annotation, this) : resolve(id, type, annotation);
	}

	@Override
	public Object resolveMultiple(final Collection<String> ids, final TypeInformation<?> type, final Ref annotation) {
		return ids.stream().map(id -> resolveOne(id, getNonNullComponentType(type), annotation))
				.collect(Collectors.toList());
	}

	@Override
	public Object resolve(final String id, final TypeInformation<?> type, final Ref annotation) {
		return template.find(id, type.getType()).get();
	}

}
