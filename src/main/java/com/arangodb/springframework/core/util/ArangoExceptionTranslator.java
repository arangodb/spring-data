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

package com.arangodb.springframework.core.util;

import org.springframework.dao.*;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import com.arangodb.ArangoDBException;
import com.arangodb.springframework.ArangoUncategorizedException;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static java.util.Map.entry;

/**
 * Translate any {@link ArangoDBException} to the appropriate {@link DataAccessException} using response code and error number.
 *
 * @see <a href="https://github.com/arangodb/arangodb/blob/devel/lib/Basics/errors.dat">General ArangoDB storage errors</a>
 * @author Mark Vollmary
 * @author Christian Lechner
 * @author Arne Burmeister
 */
public class ArangoExceptionTranslator implements PersistenceExceptionTranslator {

	@Override
	public DataAccessException translateExceptionIfPossible(final RuntimeException ex) {
		DataAccessException dae = null;
		if (ex instanceof DataAccessException exception) {
			dae = exception;
		} else if (ex instanceof ArangoDBException exception) {
			final Integer responseCode = exception.getResponseCode();
			if (responseCode != null) {
				BiFunction<String, ArangoDBException, DataAccessException> constructor = switch (responseCode) {
					case ArangoErrors.ERROR_HTTP_UNAUTHORIZED, ArangoErrors.ERROR_HTTP_FORBIDDEN -> PermissionDeniedDataAccessException::new;
					case ArangoErrors.ERROR_HTTP_BAD_PARAMETER, ArangoErrors.ERROR_HTTP_METHOD_NOT_ALLOWED -> InvalidDataAccessApiUsageException::new;
					case ArangoErrors.ERROR_HTTP_NOT_FOUND -> mostSpecific(exception, Map.ofEntries(
								entry(hasErrorNumber(1202), DataRetrievalFailureException::new)
							), InvalidDataAccessResourceUsageException::new);
					case ArangoErrors.ERROR_HTTP_CONFLICT -> mostSpecific(exception, Map.ofEntries(
								entry(hasErrorNumber(1200).and(errorMessageContains("write-write conflict")), TransientDataAccessResourceException::new),
								entry(hasErrorNumber(1200).and(errorMessageContains("_rev")), OptimisticLockingFailureException::new),
								entry(hasErrorNumber(1210).and(errorMessageContains("_key")), DuplicateKeyException::new)
							),
							DataIntegrityViolationException::new);
					case ArangoErrors.ERROR_HTTP_PRECONDITION_FAILED -> OptimisticLockingFailureException::new;
					case ArangoErrors.ERROR_HTTP_SERVICE_UNAVAILABLE -> DataAccessResourceFailureException::new;
					default -> ArangoUncategorizedException::new;
				};
				return constructor.apply(exception.getMessage(), exception);
			}
		}
		if (dae == null) {
			throw ex;
		}
		return dae;
	}

	private static BiFunction<String, ArangoDBException, DataAccessException> mostSpecific(ArangoDBException exception,
											 Map<Predicate<ArangoDBException>, BiFunction<String, ArangoDBException, DataAccessException>> specific,
											 BiFunction<String, ArangoDBException, DataAccessException> fallback) {
		return specific.entrySet().stream()
				.filter(entry -> entry.getKey().test(exception))
				.map(Map.Entry::getValue)
				.findAny()
				.orElse(fallback);
	}

	private static Predicate<ArangoDBException> hasErrorNumber(int expected) {
		return exception -> exception.getErrorNum() != null && exception.getErrorNum() == expected;
	}

	private static Predicate<ArangoDBException> errorMessageContains(String expected) {
		return exception -> exception.getErrorMessage() != null && exception.getErrorMessage().contains(expected);
	}
}
