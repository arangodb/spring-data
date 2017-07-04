package com.arangodb.springframework.core.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.util.ReflectionUtils;

class BeanWrapper<T> implements PersistentPropertyAccessor {

	private final T bean;

	/**
	 * Creates a new {@link BeanWrapper} for the given bean.
	 * 
	 * @param bean
	 *            must not be {@literal null}.
	 */
	protected BeanWrapper(final T bean) {

		this.bean = bean;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#setProperty(org.springframework.data.mapping.
	 * PersistentProperty, java.util.Optional)
	 */
	@Override
	public void setProperty(final PersistentProperty<?> property, final Optional<? extends Object> value) {

		try {

			if (!property.usePropertyAccess()) {

				final Field field = property.getField();

				ReflectionUtils.makeAccessible(field);
				ReflectionUtils.setField(field, bean, value.orElse(null));
				return;
			}

			final Optional<Method> setter = Optional.ofNullable(property.getSetter());

			setter.ifPresent(it -> {

				ReflectionUtils.makeAccessible(it);
				ReflectionUtils.invokeMethod(it, bean, value.orElse(null));
			});

		} catch (final IllegalStateException e) {
			throw new MappingException("Could not set object property!", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#getProperty(org.springframework.data.mapping.
	 * PersistentProperty)
	 */
	@Override
	public Optional<Object> getProperty(final PersistentProperty<?> property) {
		return getProperty(property, property.getType());
	}

	/**
	 * Returns the value of the given {@link PersistentProperty} potentially converted to the given type.
	 * 
	 * @param <S>
	 * @param property
	 *            must not be {@literal null}.
	 * @param type
	 *            can be {@literal null}.
	 * @return
	 * @throws MappingException
	 *             in case an exception occured when accessing the property.
	 */
	@SuppressWarnings("unchecked")
	public <S> Optional<S> getProperty(final PersistentProperty<?> property, final Class<? extends S> type) {

		try {

			if (!property.usePropertyAccess()) {

				final Field field = property.getField();
				ReflectionUtils.makeAccessible(field);
				return Optional.ofNullable((S) ReflectionUtils.getField(field, bean));
			}

			final Optional<Method> getter = Optional.ofNullable(property.getGetter());

			return getter.map(it -> {

				ReflectionUtils.makeAccessible(it);
				return (S) ReflectionUtils.invokeMethod(it, bean);
			});

		} catch (final IllegalStateException e) {
			throw new MappingException(
					String.format("Could not read property %s of %s!", property.toString(), bean.toString()), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.mapping.PersistentPropertyAccessor#getBean()
	 */
	@Override
	public T getBean() {
		return bean;
	}
}
