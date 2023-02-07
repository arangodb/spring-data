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

	@Override
	public DataAccessException translateExceptionIfPossible(final RuntimeException ex) {
		DataAccessException dae = null;
		if (ex instanceof DataAccessException) {
			dae = DataAccessException.class.cast(ex);
		} else if (ex instanceof ArangoDBException) {
			final ArangoDBException exception = ArangoDBException.class.cast(ex);
			final Integer responseCode = exception.getResponseCode();
			if (responseCode != null) {
				switch (responseCode) {
				case ArangoErrors.ERROR_HTTP_UNAUTHORIZED:
				case ArangoErrors.ERROR_HTTP_FORBIDDEN:
					dae = new PermissionDeniedDataAccessException(exception.getMessage(), exception);
					break;
				case ArangoErrors.ERROR_HTTP_BAD_PARAMETER:
				case ArangoErrors.ERROR_HTTP_METHOD_NOT_ALLOWED:
					dae = new InvalidDataAccessApiUsageException(exception.getMessage(), exception);
					break;
				case ArangoErrors.ERROR_HTTP_NOT_FOUND:
					dae = new InvalidDataAccessResourceUsageException(exception.getMessage(), exception);
					break;
				case ArangoErrors.ERROR_HTTP_CONFLICT:
					if (exception.getMessage().contains("_rev")) {
						dae = new OptimisticLockingFailureException(exception.getMessage(), exception);
					} else {
						dae = new DataIntegrityViolationException(exception.getMessage(), exception);
					}
					break;
				case ArangoErrors.ERROR_HTTP_PRECONDITION_FAILED:
					dae = new OptimisticLockingFailureException(exception.getMessage(), exception);
					break;
				case ArangoErrors.ERROR_HTTP_SERVICE_UNAVAILABLE:
					dae = new DataAccessResourceFailureException(exception.getMessage(), exception);
					break;
				case ArangoErrors.ERROR_HTTP_SERVER_ERROR:
				default:
					dae = new ArangoUncategorizedException(exception.getMessage(), exception);
					break;
				}
			}
		}
		if (dae == null) {
			throw ex;
		}
		return dae;
	}

}
