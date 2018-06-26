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

package com.arangodb.springframework.repository.query;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.geo.GeoPage;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.annotation.QueryOptions;

/**
 * 
 * @author Audrius Malele
 * @author Mark McCormick
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public class ArangoQueryMethod extends QueryMethod {

	private static final List<Class<?>> GEO_TYPES = Arrays.asList(GeoResult.class, GeoResults.class, GeoPage.class);

	private final Method method;
	private final TypeInformation<?> returnType;

	public ArangoQueryMethod(final Method method, final RepositoryMetadata metadata, final ProjectionFactory factory) {
		super(method, metadata, factory);
		this.method = method;
		this.returnType = ClassTypeInformation.from(metadata.getRepositoryInterface()).getReturnType(method);
	}

	@Override
	public ArangoParameters getParameters() {
		return (ArangoParameters) super.getParameters();
	}

	@Override
	public ArangoParameters createParameters(final Method method) {
		return new ArangoParameters(method);
	}

	public boolean hasAnnotatedQuery() {
		return getQueryAnnotationValue().isPresent();
	}

	public String getAnnotatedQuery() {
		return getQueryAnnotationValue().orElse(null);
	}

	public Query getQueryAnnotation() {
		return AnnotatedElementUtils.findMergedAnnotation(method, Query.class);
	}

	private Optional<String> getQueryAnnotationValue() {
		return Optional.ofNullable(getQueryAnnotation()) //
				.map(q -> q.value()) //
				.filter(StringUtils::hasText);
	}

	public boolean hasAnnotatedQueryOptions() {
		return getQueryOptionsAnnotation() != null;
	}

	public AqlQueryOptions getAnnotatedQueryOptions() {
		final QueryOptions queryOptions = getQueryOptionsAnnotation();
		if (queryOptions == null) {
			return null;
		}
		final AqlQueryOptions options = new AqlQueryOptions();
		final int batchSize = queryOptions.batchSize();
		if (batchSize != -1) {
			options.batchSize(batchSize);
		}
		final int maxPlans = queryOptions.maxPlans();
		if (maxPlans != -1) {
			options.maxPlans(maxPlans);
		}
		final int ttl = queryOptions.ttl();
		if (ttl != -1) {
			options.ttl(ttl);
		}
		options.cache(queryOptions.cache());
		options.count(queryOptions.count());
		options.fullCount(queryOptions.fullCount());
		options.profile(queryOptions.profile());
		options.rules(Arrays.asList(queryOptions.rules()));
		final boolean stream = queryOptions.stream();
		if (stream) {
			options.stream(stream);
		}
		final long memoryLimit = queryOptions.memoryLimit();
		if (memoryLimit != -1) {
			options.memoryLimit(memoryLimit);
		}
		return options;
	}

	public QueryOptions getQueryOptionsAnnotation() {
		return AnnotatedElementUtils.findMergedAnnotation(method, QueryOptions.class);
	}

	public TypeInformation<?> getReturnType() {
		return returnType;
	}

	public boolean isGeoQuery() {
		final Class<?> returnType = method.getReturnType();
		for (final Class<?> type : GEO_TYPES) {
			if (type.isAssignableFrom(returnType)) {
				return true;
			}
		}
		return false;
	}

}
