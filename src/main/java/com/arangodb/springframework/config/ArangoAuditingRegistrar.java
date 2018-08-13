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

package com.arangodb.springframework.config;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.auditing.IsNewAwareAuditingHandler;
import org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport;
import org.springframework.data.auditing.config.AuditingConfiguration;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.Assert;

import com.arangodb.springframework.annotation.EnableArangoAuditing;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;
import com.arangodb.springframework.core.mapping.event.AuditingEventListener;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoAuditingRegistrar extends AuditingBeanDefinitionRegistrarSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableArangoAuditing.class;
	}

	@Override
	protected void registerAuditListenerBeanDefinition(
		final BeanDefinition auditingHandlerDefinition,
		final BeanDefinitionRegistry registry) {

		Assert.notNull(auditingHandlerDefinition, "BeanDefinition must not be null!");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");

		final BeanDefinitionBuilder listenerBeanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(AuditingEventListener.class);
		listenerBeanDefinitionBuilder.addConstructorArgValue(
			ParsingUtils.getObjectFactoryBeanDefinition(getAuditingHandlerBeanName(), registry));

		registerInfrastructureBeanWithId(listenerBeanDefinitionBuilder.getBeanDefinition(),
			AuditingEventListener.class.getName(), registry);
	}

	@Override
	protected String getAuditingHandlerBeanName() {
		return "arangoAuditingHandler";
	}

	@Override
	protected BeanDefinitionBuilder getAuditHandlerBeanDefinitionBuilder(final AuditingConfiguration configuration) {

		Assert.notNull(configuration, "AuditingConfiguration must not be null!");

		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(IsNewAwareAuditingHandler.class);

		final BeanDefinitionBuilder definition = BeanDefinitionBuilder
				.genericBeanDefinition(ArangoMappingContextLookup.class);
		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);

		builder.addConstructorArgValue(definition.getBeanDefinition());
		return configureDefaultAuditHandlerAttributes(configuration, builder);
	}

	static class ArangoMappingContextLookup
			implements FactoryBean<MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty>> {

		private final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context;

		public ArangoMappingContextLookup(
			final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context) {
			super();
			this.context = context;
		}

		@Override
		public MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> getObject()
				throws Exception {
			return context;
		}

		@Override
		public Class<?> getObjectType() {
			return MappingContext.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

	}

}
