package com.arangodb.springframework.core.convert.resolver;

import java.lang.annotation.Annotation;
import java.util.Optional;

import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.annotation.To;
import com.arangodb.springframework.core.ArangoOperations;

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
