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

import com.arangodb.springframework.repository.query.QueryTransactionBridge;
import org.springframework.data.util.TypeInformation;

import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.core.ArangoOperations;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * @author Mark Vollmary
 */
public class EdgeFromResolver extends AbstractResolver implements RelationResolver<From> {

	private final ArangoOperations template;

	public EdgeFromResolver(final ArangoOperations template, QueryTransactionBridge transactionBridge) {
		super(template.getConverter().getConversionService(), transactionBridge);
		this.template = template;
	}

	@Override
    public Object resolveOne(final String id, final TypeInformation<?> type, Collection<TypeInformation<?>> traversedTypes,
                             final From annotation) {
        Supplier<Object> supplier = () -> _resolveOne(id, type);
        return annotation.lazy() ? proxy(id, type, supplier) : supplier.get();
	}

	private Object _resolveOne(final String id, final TypeInformation<?> type) {
		return template.find(id, type.getType(), defaultReadOptions())
				.orElseThrow(() -> cannotResolveException(id, type));
	}

	@Override
    public Object resolveMultiple(final String id, final TypeInformation<?> type, Collection<TypeInformation<?>> traversedTypes,
                                  final From annotation) {
		throw new UnsupportedOperationException("Edges with multiple 'from' values are not supported.");
	}

}
