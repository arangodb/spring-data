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
import com.arangodb.springframework.annotation.To;
import com.arangodb.springframework.core.ArangoOperations;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public class DocumentToResolver extends AbstractResolver implements RelationResolver<To> {

	private final ArangoOperations template;

    public DocumentToResolver(final ArangoOperations template) {
		super(template.getConverter().getConversionService());
		this.template = template;
    }

    @Override
    public Object resolveOne(final String id, final TypeInformation<?> type, Collection<TypeInformation<?>> traversedTypes,
                             final To annotation) {
        Supplier<Object> supplier = () -> _resolveOne(id, type);
        return annotation.lazy() ? proxy(id, type, supplier) : supplier.get();
    }

    private Object _resolveOne(final String id, final TypeInformation<?> type) {
        ArangoCursor<?> it = _resolve(id, type.getType(), true);
        return it.hasNext() ? it.next() : null;
    }

    @Override
    public Object resolveMultiple(final String id, final TypeInformation<?> type, Collection<TypeInformation<?>> traversedTypes,
                                  final To annotation) {
        Supplier<Object> supplier = () -> _resolveMultiple(id, type);
        return annotation.lazy() ? proxy(id, type, supplier) : supplier.get();
    }

    private Object _resolveMultiple(final String id, final TypeInformation<?> type) {
        return _resolve(id, getNonNullComponentType(type).getType(), false).asListRemaining();
    }

    private ArangoCursor<?> _resolve(final String id, final Class<?> type, final boolean limit) {
        final String query = String.format("FOR e IN @@edge FILTER e._to == @id %s RETURN e", limit ? "LIMIT 1" : "");
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("@edge", type);
        bindVars.put("id", id);
		return template.query(query, bindVars, defaultQueryOptions(), type);
    }

}
