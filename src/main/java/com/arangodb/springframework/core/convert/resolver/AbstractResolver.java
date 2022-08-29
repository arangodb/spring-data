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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.function.Supplier;

import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.springframework.repository.query.QueryTransactionBridge;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.util.ReflectionUtils;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public abstract class AbstractResolver {

	private static final Method GET_ENTITY_METHOD;
	private static final Method GET_REF_ID_METHOD;
	private static final Method IS_RESOLVED;

	static {
		try {
			GET_ENTITY_METHOD = LazyLoadingProxy.class.getMethod("getEntity");
			GET_REF_ID_METHOD = LazyLoadingProxy.class.getMethod("getRefId");
			IS_RESOLVED = LazyLoadingProxy.class.getMethod("isResolved");
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private final ObjenesisStd objenesis;
	private final ConversionService conversionService;
    private final QueryTransactionBridge transactionBridge;

	protected AbstractResolver(final ConversionService conversionService, final QueryTransactionBridge transactionBridge) {
		super();
		this.conversionService = conversionService;
        this.transactionBridge = transactionBridge;
		this.objenesis = new ObjenesisStd(true);
	}

	protected Object proxy(
		final String id,
		final TypeInformation<?> type,
		final Supplier<Object> callback) {
        final ProxyInterceptor interceptor = new ProxyInterceptor(id, type, callback, conversionService);
		if (type.getType().isInterface()) {
			final ProxyFactory proxyFactory = new ProxyFactory(new Class<?>[] { type.getType() });
			for (final Class<?> interf : type.getType().getInterfaces()) {
				proxyFactory.addInterface(interf);
			}
			proxyFactory.addInterface(LazyLoadingProxy.class);
			proxyFactory.addAdvice(interceptor);
			return proxyFactory.getProxy();
		} else {
			final Factory factory = (Factory) objenesis.newInstance(enhancedTypeFor(type.getType()));
			factory.setCallbacks(new Callback[] { interceptor });
			return factory;
		}
	}

	private Class<?> enhancedTypeFor(final Class<?> type) {
		final Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(type);
		enhancer.setCallbackType(org.springframework.cglib.proxy.MethodInterceptor.class);
		enhancer.setInterfaces(new Class[] { LazyLoadingProxy.class });
		return enhancer.createClass();
	}

    protected DocumentReadOptions defaultReadOptions() {
        DocumentReadOptions options = new DocumentReadOptions();
		if (transactionBridge != null) {
			options.streamTransactionId(transactionBridge.getCurrentTransaction(Collections.emptySet()));
		}
        return options;
    }

    protected AqlQueryOptions defaultQueryOptions() {
        AqlQueryOptions options = new AqlQueryOptions();
		if (transactionBridge != null) {
			options.streamTransactionId(transactionBridge.getCurrentTransaction(Collections.emptySet()));
		}
        return options;
    }

    static class ProxyInterceptor implements Serializable,
			org.springframework.cglib.proxy.MethodInterceptor, org.aopalliance.intercept.MethodInterceptor {

		private static final long serialVersionUID = -6722757823918987065L;
		private final String id;
		final TypeInformation<?> type;
        private final Supplier<Object> callback;
		private volatile boolean resolved;
		private Object result;
		private final ConversionService conversionService;

        public ProxyInterceptor(final String id, final TypeInformation<?> type,
                                final Supplier<Object> callback, final ConversionService conversionService) {
			super();
			this.id = id;
			this.type = type;
			this.callback = callback;
			this.conversionService = conversionService;
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

			if (GET_ENTITY_METHOD.equals(method)) {
				return ensureResolved();
			}

			if (GET_REF_ID_METHOD.equals(method)) {
				return id;
			}

			if (IS_RESOLVED.equals(method)) {
				return resolved;
			}

			if (method.getName().equals("canEqual")) {
				return proxyCanEqual(ensureResolved(), args[0]);
			}
			if (ReflectionUtils.isObjectMethod(method)) {
				if (ReflectionUtils.isToStringMethod(method)) {
					return proxyToString();
				}

				else if (ReflectionUtils.isEqualsMethod(method)) {
					return proxyEquals(proxy, args[0]);
				}

				else if (ReflectionUtils.isHashCodeMethod(method)) {
					return proxyHashCode();
				}
			}

			final Object result = ensureResolved();
			return result == null ? null : method.invoke(result, args);
		}

		private Object ensureResolved() {
			if (!resolved) {
				result = resolve();
				resolved = true;
			}
			return result;
		}

		private synchronized Object resolve() {
			if (!resolved) {
                return convertIfNecessary(callback.get(), type.getType());
			}
			return result;
		}

		private String proxyToString() {
			return new StringBuilder()
					.append(LazyLoadingProxy.class.getSimpleName())
					.append(" [")
					.append(id)
					.append("]")
					.toString();
		}

		private int proxyHashCode() {
			return proxyToString().hashCode();
		}

		private boolean proxyCanEqual(final Object proxy, final Object obj) {
			return obj.getClass().isInstance(proxy);
		}

		private boolean proxyEquals(final Object proxy, final Object obj) {
			if (!(obj instanceof LazyLoadingProxy)) {
				return false;
			}
			if (obj == proxy) {
				return true;
			}
			return proxyToString().equals(obj.toString());
		}

		@SuppressWarnings("unchecked")
		private <T> T convertIfNecessary(@Nullable final Object source, final Class<T> type) {
			return (T) (source == null ? null
					: type.isAssignableFrom(source.getClass()) ? source : conversionService.convert(source, type));
		}
	}

	protected static TypeInformation<?> getNonNullComponentType(final TypeInformation<?> type) {
		final TypeInformation<?> compType = type.getComponentType();
		return compType != null ? compType : TypeInformation.OBJECT;
	}

	protected static RuntimeException cannotResolveException(final String id, final TypeInformation<?> type) {
		return new IllegalArgumentException("Cannot resolve " + type.getType() + " for id " + id);
	}

}
