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

import com.arangodb.springframework.annotation.To;
import com.arangodb.springframework.core.ArangoOperations;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * @author Mark Vollmary
 */
public class EdgeToResolver extends AbstractResolver implements RelationResolver<To> {

	public EdgeToResolver(final ArangoOperations template) {
		super(template);
	}

	@Override
    public Object resolveOne(final String id, final TypeInformation<?> type, Collection<TypeInformation<?>> traversedTypes,
                             final To annotation) {
        Supplier<Object> supplier = () -> _resolveOne(id, type);
        return annotation.lazy() ? proxy(id, type, supplier) : supplier.get();
	}

	private Object _resolveOne(final String id, final TypeInformation<?> type) {
		return template.find(id, type.getType())
				.orElseThrow(() -> cannotResolveException(id, type));
	}

	@Override
	public Object resolveMultiple(final String id, final TypeInformation<?> type, Collection<TypeInformation<?>> traversedTypes, final To annotation) {
		throw new UnsupportedOperationException("Edges with multiple 'to' values are not supported.");
	}

}
