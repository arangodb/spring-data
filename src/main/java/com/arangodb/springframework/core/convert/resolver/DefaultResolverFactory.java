/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
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
import java.util.Optional;

import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.annotation.To;
import com.arangodb.springframework.core.ArangoOperations;

/**
 * 
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public class DefaultResolverFactory implements ResolverFactory {

	private final RefResolver refResolver;
	private final RelationsResolver relationsResolver;
	private final FromResolver fromResolver;
	private final ToResolver toResolver;

	public DefaultResolverFactory(final ArangoOperations template) {
		refResolver = new RefResolver(template);
		relationsResolver = new RelationsResolver(template);
		fromResolver = new FromResolver(template);
		toResolver = new ToResolver(template);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Optional<ReferenceResolver<A>> getReferenceResolver(final A annotation) {
		ReferenceResolver<A> resolver = null;
		if (Ref.class.equals(annotation)) {
			resolver = (ReferenceResolver<A>) refResolver;
		}
		return Optional.ofNullable(resolver);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A extends Annotation> Optional<RelationResolver<A>> getRelationResolver(final A annotation) {
		RelationResolver<A> resolver = null;
		if (From.class.equals(annotation)) {
			resolver = (RelationResolver<A>) fromResolver;
		} else if (To.class.equals(annotation)) {
			resolver = (RelationResolver<A>) toResolver;
		} else if (Relations.class.equals(annotation)) {
			resolver = (RelationResolver<A>) relationsResolver;
		}
		return Optional.ofNullable(resolver);
	}

}
