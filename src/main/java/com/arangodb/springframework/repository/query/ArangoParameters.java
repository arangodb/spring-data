/*
 * DISCLAIMER
 *
 * Copyright 2017 ArangoDB GmbH, Cologne, Germany
 * Copyright 2023 Hewlett Packard Enterprise Development LP.
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
 * Copyright holder is Hewlett Packard Enterprise Development LP.
 */

package com.arangodb.springframework.repository.query;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.arangodb.springframework.annotation.SpelParam;
import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.util.Assert;
import com.arangodb.springframework.annotation.Filter;
import com.arangodb.springframework.repository.query.filter.Filterable;
import com.arangodb.springframework.repository.query.search.Searchable;

import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.annotation.BindVars;

/**
 *
 * @author Audrius Malele
 * @author Mark McCormick
 * @author Mark Vollmary
 * @author Christian Lechner
 * @author Siva Prasad Erigineni
 */
public class ArangoParameters extends Parameters<ArangoParameters, ArangoParameters.ArangoParameter> {

	private final int queryOptionsIndex;
	private final int bindVarsIndex;
	private final Map<String, Integer> filterIndexes;
	private final int searchIndex;

	public ArangoParameters(final Method method) {
		super(method);
		assertSingleSpecialParameter(ArangoParameter::isQueryOptions,
			"Multiple AqlQueryOptions parameters are not allowed! Offending method: " + method);
		assertSingleSpecialParameter(ArangoParameter::isBindVars,
			"Multiple @BindVars parameters are not allowed! Offending method: " + method);
		assertNonDuplicateParamNames(method);
		this.queryOptionsIndex = getIndexOfSpecialParameter(ArangoParameter::isQueryOptions);
		this.bindVarsIndex = getIndexOfSpecialParameter(ArangoParameter::isBindVars);
		this.filterIndexes = mapFilterIndexes();
		this.searchIndex = getIndexOfSpecialParameter(ArangoParameter::isSearch);

	}

	private ArangoParameters(final List<ArangoParameter> parameters, final int queryOptionsIndex,
		final int bindVarsIndex) {
		super(parameters);
		this.queryOptionsIndex = queryOptionsIndex;
		this.bindVarsIndex = bindVarsIndex;
		this.filterIndexes = filterIndexes;
		this.searchIndex = searchIndex;
	}

	@Override
	protected ArangoParameter createParameter(final MethodParameter parameter) {
		return new ArangoParameter(parameter);
	}

	@Override
	protected ArangoParameters createFrom(final List<ArangoParameter> parameters) {
		return new ArangoParameters(parameters, this.queryOptionsIndex, this.bindVarsIndex, this.filterIndexes, this.searchIndex);
	}

	public boolean hasQueryOptions() {
		return this.queryOptionsIndex != -1;
	}

	public int getQueryOptionsIndex() {
		return this.queryOptionsIndex;
	}

	public boolean hasBindVars() {
		return this.bindVarsIndex != -1;
	}

	public int getBindVarsIndex() {
		return this.bindVarsIndex;
	}
	public boolean hasFilterParameter() { return this.filterIndexes.size() > 0; }

	public Map<String, Integer> getFilterIndexes() { return this.filterIndexes; }

	private int getIndexOfSpecialParameter(final Predicate<ArangoParameter> condition) {
		for (int index = 0; index < getNumberOfParameters(); ++index) {
			final ArangoParameter param = getParameter(index);
			if (condition.test(param)) {
				return index;
			}
		}
		return -1;
	}

	public boolean hasSearchParameter() { return this.searchIndex != -1; }

	public int getSearchIndex() {
		return this.searchIndex;
	}

	/**
	 * Returns the index of all Filter parameters indexed by their corresponding placeholders.
	 */
	private Map<String, Integer> mapFilterIndexes() {
		Map<String, Integer> filters = new HashMap<>();

		for (int index = 0; index < getNumberOfParameters(); ++index) {
			final ArangoParameter param = getParameter(index);
			if (param.isFilter()) {
				filters.put(param.getPlaceholder(), index);
			}
		}

		return filters;
	}


	private void assertSingleSpecialParameter(final Predicate<ArangoParameter> condition, final String message) {
		boolean found = false;
		for (int index = 0; index < getNumberOfParameters(); ++index) {
			final ArangoParameter param = getParameter(index);
			if (condition.test(param)) {
				Assert.isTrue(!found, message);
				found = true;
			}
		}
	}

	private void assertNonDuplicateParamNames(final Method method) {
		final ArangoParameters bindableParams = getBindableParameters();
		final int bindableParamsSize = bindableParams.getNumberOfParameters();
		final Set<String> paramNames = new HashSet<>(bindableParamsSize);
		for (int i = 0; i < bindableParamsSize; ++i) {
			final ArangoParameter param = bindableParams.getParameter(i);
			final Optional<String> name = param.getName();
			if (name.isPresent()) {
				Assert.isTrue(!paramNames.contains(name.get()),
					"Duplicate parameter name! Offending method: " + method);
				paramNames.add(name.get());
			}
		}
	}

	static class ArangoParameter extends Parameter {

		private static final Pattern BIND_PARAM_PATTERN = Pattern.compile("^@?[A-Za-z0-9][A-Za-z0-9_]*$");
		private static final String NAMED_PARAMETER_TEMPLATE = "@%s";
		private static final String POSITION_PARAMETER_TEMPLATE = "@%d";

		private final MethodParameter parameter;

		public ArangoParameter(final MethodParameter parameter) {
			super(parameter);
			this.parameter = parameter;
			assertCorrectBindParamPattern();
			assertCorrectBindVarsType();
			assertFilterType();
		}

		@Override
		public boolean isSpecialParameter() {
			return super.isSpecialParameter() || isQueryOptions() || isBindVars() || isSpelParam();
		}

		public boolean isQueryOptions() {
			return AqlQueryOptions.class.isAssignableFrom(parameter.getParameterType());
		}

		public boolean isBindVars() {
			return parameter.hasParameterAnnotation(BindVars.class);
		}

		public boolean isSpelParam() {
			return parameter.hasParameterAnnotation(SpelParam.class);
		}
		public boolean isFilter() { return parameter.hasParameterAnnotation(Filter.class); }

		public boolean isSearch() {
			return Searchable.class.isAssignableFrom(parameter.getParameterType());
		}

        @Override
        public Optional<String> getName() {
            if (isSpelParam())
                return Optional.of(parameter.getParameterAnnotation(SpelParam.class).value());
            else
                return super.getName();
        }

		@Override
		public String getPlaceholder() {
			if (isNamedParameter()) {
				return String.format(NAMED_PARAMETER_TEMPLATE, getName().get());
			} 
			else if (isFilter()) {
				return this.parameter.getParameterAnnotation(Filter.class).value();
			}  
			else {
				return String.format(POSITION_PARAMETER_TEMPLATE, getIndex());
			}
		}

		private void assertCorrectBindParamPattern() {
			if (isExplicitlyNamed()) {
				final String name = getName().get();
				final boolean matches = BIND_PARAM_PATTERN.matcher(name).matches();
				Assert.isTrue(matches, "@Param has invalid format! Offending parameter: parameter "
						+ parameter.getParameterIndex() + " on method " + parameter.getMethod());
			}
		}

		private void assertCorrectBindVarsType() {
			final String errorMsg = "@BindVars parameter must be of type Map<String, Object>! Offending parameter: parameter "
					+ parameter.getParameterIndex() + " on method " + parameter.getMethod();

			if (isBindVars()) {
				Assert.isTrue(Map.class.equals(parameter.getParameterType()), errorMsg);

				final Type type = parameter.getGenericParameterType();
				Assert.isTrue(ParameterizedType.class.isInstance(type), errorMsg);

				final Type[] genericTypes = ((ParameterizedType) type).getActualTypeArguments();
				Assert.isTrue(genericTypes.length == 2, errorMsg);

				final Type keyType = genericTypes[0];
				final Type valueType = genericTypes[1];

				Assert.isTrue(Class.class.isInstance(keyType), errorMsg);
				Assert.isTrue(Class.class.isInstance(valueType), errorMsg);
				Assert.isTrue(String.class.equals(keyType), errorMsg);
				Assert.isTrue(Object.class.equals(valueType), errorMsg);
			}
		}
		private void assertFilterType() {
			final String errorMsg = "@Filter parameter must be of type Filterable! Offending parameter: parameter "
					+ parameter.getParameterIndex() + " on method" + parameter.getMethod();

			if (isFilter()) {
				Assert.isTrue(Filterable.class.equals(parameter.getParameterType()), errorMsg);
			}
		}

	}
}
