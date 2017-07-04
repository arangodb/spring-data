package com.arangodb.springframework.core.mapping;

import java.util.Optional;

import org.springframework.data.mapping.PersistentProperty;

public interface PersistentPropertyAccessor {

	/**
	 * Sets the given {@link PersistentProperty} to the given value. Will do type conversion if a
	 * {@link org.springframework.core.convert.ConversionService} is configured.
	 * 
	 * @param property
	 *            must not be {@literal null}.
	 * @param value
	 *            can be {@literal null}.
	 * @throws org.springframework.data.mapping.model.MappingException
	 *             in case an exception occurred when setting the property value.
	 */
	void setProperty(PersistentProperty<?> property, Optional<? extends Object> value);

	/**
	 * Returns the value of the given {@link PersistentProperty} of the underlying bean instance.
	 * 
	 * @param <S>
	 * @param property
	 *            must not be {@literal null}.
	 * @return can be {@literal null}.
	 */
	Optional<Object> getProperty(PersistentProperty<?> property);

	/**
	 * Returns the underlying bean.
	 * 
	 * @return will never be {@literal null}.
	 */
	Object getBean();
}
