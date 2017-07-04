package com.arangodb.springframework.core.mapping;

import org.springframework.data.mapping.PersistentEntity;

public interface PersistentPropertyAccessorFactory {

	/**
	 * Returns a {@link PersistentPropertyAccessor} for a given {@link PersistentEntity} and {@code bean}.
	 *
	 * @param entity
	 *            must not be {@literal null}.
	 * @param bean
	 *            must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	PersistentPropertyAccessor getPropertyAccessor(PersistentEntity<?, ?> entity, Object bean);

	/**
	 * Returns whether given {@link PersistentEntity} is supported by this {@link PersistentPropertyAccessorFactory}.
	 *
	 * @param entity
	 *            must not be {@literal null}.
	 * @return
	 */
	boolean isSupported(PersistentEntity<?, ?> entity);
}
