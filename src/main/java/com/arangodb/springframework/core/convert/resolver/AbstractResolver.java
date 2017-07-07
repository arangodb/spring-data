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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.objenesis.ObjenesisStd;

/**
 * @author Mark Vollmary
 *
 */
public abstract class AbstractResolver<A extends Annotation> {

	private final ObjenesisStd objenesis;

	protected AbstractResolver() {
		super();
		this.objenesis = new ObjenesisStd(true);
	}

	static interface ResolverCallback<A extends Annotation> {

		Object resolve(String id, Class<?> type, A annotation);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Object proxy(
		final String id,
		final Class<?> type,
		final A annotation,
		final ResolverCallback<A> callback) {
		final ProxyInterceptor interceptor = new ProxyInterceptor(id, type, annotation, callback);
		if (type.isInterface()) {
			final ProxyFactory proxyFactory = new ProxyFactory(new Class<?>[] { type });
			for (final Class<?> interf : type.getInterfaces()) {
				proxyFactory.addInterface(interf);
			}
			proxyFactory.addAdvice(interceptor);
			return proxyFactory.getProxy();
		} else {
			final Factory factory = (Factory) objenesis.newInstance(enhancedTypeFor(type));
			factory.setCallbacks(new Callback[] { interceptor });
			return factory;
		}
	}

	private Class<?> enhancedTypeFor(final Class<?> type) {
		final Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(type);
		enhancer.setCallbackType(org.springframework.cglib.proxy.MethodInterceptor.class);
		return enhancer.createClass();
	}

	static class ProxyInterceptor<A extends Annotation> implements Serializable,
			org.springframework.cglib.proxy.MethodInterceptor, org.aopalliance.intercept.MethodInterceptor {

		private static final long serialVersionUID = -6722757823918987065L;
		private final String id;
		private final Class<?> type;
		private final A annotation;
		private final ResolverCallback<A> callback;
		private volatile boolean resolved;
		private Object result;

		public ProxyInterceptor(final String id, final Class<?> type, final A annotation,
			final ResolverCallback<A> callback) {
			super();
			this.id = id;
			this.type = type;
			this.annotation = annotation;
			this.callback = callback;
			result = null;
			resolved = false;
		}

		@Override
		public Object invoke(final MethodInvocation invocation) throws Throwable {
			return intercept(invocation.getThis(), invocation.getMethod(), invocation.getArguments(), null);
		}

		@Override
		public Object intercept(final Object obj, final Method method, final Object[] args, final MethodProxy proxy)
				throws Throwable {
			final Object result = resolve();
			return result == null ? null : method.invoke(result, args);
		}

		private synchronized Object resolve() {
			if (!resolved) {
				result = callback.resolve(id, type, annotation);
				resolved = true;
			}
			return result;
		}

	}
}
