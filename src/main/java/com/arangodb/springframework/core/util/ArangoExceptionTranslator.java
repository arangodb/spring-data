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

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 *
 */
public class ArangoExceptionTranslator implements PersistenceExceptionTranslator {

	private static final Integer ERROR_ARANGO_CONFLICT = 1200;

	@Override
	public DataAccessException translateExceptionIfPossible(final RuntimeException ex) {
		DataAccessException dae = null;
		if (ex instanceof DataAccessException) {
			return DataAccessException.class.cast(ex);
		}
		if (ex instanceof ArangoDBException) {
			final ArangoDBException exception = ArangoDBException.class.cast(ex);
			final Integer responseCode = exception.getResponseCode();
			if (responseCode == null) {
				return null;
			}
			switch (responseCode) {
				case ArangoErrors.ERROR_HTTP_UNAUTHORIZED:
				case ArangoErrors.ERROR_HTTP_FORBIDDEN:
					return new PermissionDeniedDataAccessException(exception.getMessage(), exception);
				case ArangoErrors.ERROR_HTTP_BAD_PARAMETER:
				case ArangoErrors.ERROR_HTTP_METHOD_NOT_ALLOWED:
					return new InvalidDataAccessApiUsageException(exception.getMessage(), exception);
				case ArangoErrors.ERROR_HTTP_NOT_FOUND:
					return new InvalidDataAccessResourceUsageException(exception.getMessage(), exception);
				case ArangoErrors.ERROR_HTTP_CONFLICT:
					if (ERROR_ARANGO_CONFLICT.equals(exception.getErrorNum()) && exception.getMessage().contains("_rev")) {
						return new OptimisticLockingFailureException(exception.getMessage(), exception);
					} else if (ERROR_ARANGO_CONFLICT.equals(exception.getErrorNum()) && exception.getMessage().contains("write-write conflict")) {
						return new TransientDataAccessResourceException(exception.getMessage(), exception);
					} else {
						return new DataIntegrityViolationException(exception.getMessage(), exception);
					}
				case ArangoErrors.ERROR_HTTP_PRECONDITION_FAILED:
					return new OptimisticLockingFailureException(exception.getMessage(), exception);
				case ArangoErrors.ERROR_HTTP_SERVICE_UNAVAILABLE:
					return new DataAccessResourceFailureException(exception.getMessage(), exception);
				default:
					return new ArangoUncategorizedException(exception.getMessage(), exception);
			}
		}
		return null;
	}

}
