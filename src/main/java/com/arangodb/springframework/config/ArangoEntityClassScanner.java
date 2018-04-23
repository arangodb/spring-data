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

package com.arangodb.springframework.config;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Edge;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoEntityClassScanner {

	@SuppressWarnings("unchecked")
	private static final Class<? extends Annotation>[] ENTITY_ANNOTATIONS = new Class[] { Document.class, Edge.class };
	private static final AnnotationTypeFilter[] ANNOTATION_TYPE_FILTERS = new AnnotationTypeFilter[ENTITY_ANNOTATIONS.length];
	static {
		for (byte i = 0; i < ENTITY_ANNOTATIONS.length; i++)
			ANNOTATION_TYPE_FILTERS[i] = new AnnotationTypeFilter(ENTITY_ANNOTATIONS[i]);
	}

	public static Set<Class<?>> scanForEntities(final String... basePackages) throws ClassNotFoundException {
		return scanForEntities(null, basePackages);
	}
	
	public static Set<Class<?>> scanForEntities(final Class<?> assignableTo, final String... basePackages) throws ClassNotFoundException {
		final Set<Class<?>> entities = new HashSet<>();
		for (final String basePackage : basePackages) {
			entities.addAll(scanForEntities(assignableTo, basePackage));
		}
		return entities;
	}

	public static Set<Class<?>> scanForEntities(final String basePackage) throws ClassNotFoundException {
		return scanForEntities(null, basePackage);
	}

	public static Set<Class<?>> scanForEntities(final Class<?> assignableTo, final String basePackage) throws ClassNotFoundException {
		final Set<Class<?>> entities = new HashSet<>();
		if (StringUtils.hasText(basePackage)) {
			final ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(
					false);
			for (final AnnotationTypeFilter annotationTypeFilter : ANNOTATION_TYPE_FILTERS) {
				componentProvider.addIncludeFilter(annotationTypeFilter);
			}
			if (assignableTo != null)
				componentProvider.addIncludeFilter(new AssignableTypeFilter(assignableTo));
			for (final BeanDefinition definition : componentProvider.findCandidateComponents(basePackage)) {
				entities.add(ClassUtils.forName(definition.getBeanClassName(), null));
			}
		}
		return entities;
	}
}
