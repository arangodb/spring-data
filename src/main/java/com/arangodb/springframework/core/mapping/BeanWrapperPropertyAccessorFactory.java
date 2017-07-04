package com.arangodb.springframework.core.mapping;

import org.springframework.data.mapping.PersistentEntity;

enum BeanWrapperPropertyAccessorFactory implements PersistentPropertyAccessorFactory {

	INSTANCE;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.mapping.model.PersistentPropertyAccessorFactory#getPropertyAccessor(org.springframework.
	 * data.mapping.PersistentEntity, java.lang.Object)
	 */
	@Override
	public PersistentPropertyAccessor getPropertyAccessor(final PersistentEntity<?, ?> entity, final Object bean) {
		return new BeanWrapper<>(bean);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.mapping.model.PersistentPropertyAccessorFactory#isSupported(org.springframework.data.
	 * mapping.PersistentEntity)
	 */
	@Override
	public boolean isSupported(final PersistentEntity<?, ?> entity) {
		return true;
	}
}
