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

import java.lang.annotation.Annotation;
import java.util.Collection;

import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import org.springframework.data.util.TypeInformation;

/**
 * @author Mark Vollmary
 *
 */
public interface ReferenceResolver<A extends Annotation> {

	Object resolveOne(String id, TypeInformation<?> type, A annotation);

	Object resolveMultiple(Collection<String> ids, TypeInformation<?> type, A annotation);

	public String write(Object source, ArangoPersistentEntity<?> entity, Object id, Ref annotation);

}
