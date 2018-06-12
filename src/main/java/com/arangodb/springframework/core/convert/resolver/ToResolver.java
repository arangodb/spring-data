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

import org.springframework.core.convert.ConversionService;
import org.springframework.data.util.TypeInformation;

import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.annotation.To;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.util.MapBuilder;

/**
 * @author Mark Vollmary
 *
 */
public class ToResolver extends AbstractResolver<To> implements RelationResolver<To> {

	private final ArangoOperations template;

	public ToResolver(final ArangoOperations template, final ConversionService conversionService) {
		super(conversionService);
		this.template = template;
	}

	@Override
	public Object resolveOne(final String id, final TypeInformation<?> type, final To annotation) {
		return annotation.lazy() ? proxy(id, type, annotation, (i, t, a) -> internalResolveOne(i, t))
				: internalResolveOne(id, type);
	}

	private Object internalResolveOne(final String id, final TypeInformation<?> type) {
		return template.find(id, type.getType()).get();
	}

	@Override
	public Object resolveMultiple(final String id, final TypeInformation<?> type, final To annotation) {
		return annotation.lazy() ? proxy(id, type, annotation, (i, t, a) -> internalResolveMultiple(i, t))
				: internalResolveMultiple(id, type);
	}

	private Object internalResolveMultiple(final String id, final TypeInformation<?> type) {
		final Class<?> t = getNonNullComponentType(type).getType();
		return template.query("FOR e IN @@edge FILTER e._to == @id RETURN e",
			new MapBuilder().put("@edge", t).put("id", id).get(), new AqlQueryOptions(), t).asListRemaining();
	}

}
