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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.util.Assert;

import com.arangodb.model.AqlQueryOptions;

/**
 * The main class used to access parameters of a user-defined method. It wraps ParametersParameterAccessor which catches
 * special parameters Sort and Pageable, and catches Arango-specific parameters e.g. AqlQueryOptions
 */
public class ArangoParameterAccessor implements ParameterAccessor {
	private final ParametersParameterAccessor accessor;
	private final List<Object> bindableArguments;
	private AqlQueryOptions options = null;

	public ArangoParameterAccessor(final ArangoParameters parameters, final Object[] arguments) {
		accessor = new ParametersParameterAccessor(parameters, arguments);
		this.bindableArguments = createBindableArguments(arguments);
	}

	AqlQueryOptions getAqlQueryOptions() {
		return options;
	}

	@Override
	public Pageable getPageable() {
		return accessor.getPageable();
	}

	@Override
	public Sort getSort() {
		return accessor.getSort();
	}

	@Override
	public Class<?> getDynamicProjection() {
		return accessor.getDynamicProjection();
	}

	@Override
	public Object getBindableValue(final int index) {
		return accessor.getBindableValue(index);
	}

	@Override
	public boolean hasBindableNullValue() {
		return accessor.hasBindableNullValue();
	}

	@Override
	public Iterator<Object> iterator() {
		return bindableArguments.iterator();
	}

	private List<Object> createBindableArguments(final Object[] arguments) {
		final List<Object> bindableArguments = new LinkedList<>();
		for (final Parameter parameter : accessor.getParameters().getBindableParameters()) {
			if (parameter.getType() == AqlQueryOptions.class) {
				Assert.isTrue(options == null, "AqlQueryOptions duplicated");
				options = (AqlQueryOptions) arguments[parameter.getIndex()];
			} else {
				bindableArguments.add(arguments[parameter.getIndex()]);
			}
		}
		return bindableArguments;
	}
}
