/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
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

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import com.arangodb.ArangoDBException;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoExceptionTranslator implements PersistenceExceptionTranslator {

	@Override
	public DataAccessException translateExceptionIfPossible(final RuntimeException ex) {
		DataAccessException dae = null;
		if (DataAccessException.class.isAssignableFrom(ex.getClass())) {
			dae = DataAccessException.class.cast(ex);
		} else if (ArangoDBException.class.isAssignableFrom(ex.getClass())) {
			final ArangoDBException exception = ArangoDBException.class.cast(ex);
			final int responseCode = exception.getResponseCode();
			switch (responseCode) {
			case ArangoErrors.ERROR_HTTP_BAD_PARAMETER:
				// TODO
				break;
			case ArangoErrors.ERROR_HTTP_UNAUTHORIZED:
				dae = new PermissionDeniedDataAccessException(exception.getMessage(), exception);
				break;
			case ArangoErrors.ERROR_HTTP_FORBIDDEN:
				// TODO
				break;
			case ArangoErrors.ERROR_HTTP_NOT_FOUND:
				dae = new InvalidDataAccessApiUsageException(exception.getMessage(), exception);
				break;
			case ArangoErrors.ERROR_HTTP_METHOD_NOT_ALLOWED:
				// TODO
				break;
			case ArangoErrors.ERROR_HTTP_PRECONDITION_FAILED:
				// TODO
				break;
			case ArangoErrors.ERROR_HTTP_SERVER_ERROR:
				// TODO
				break;
			case ArangoErrors.ERROR_HTTP_SERVICE_UNAVAILABLE:
				dae = new DataAccessResourceFailureException(exception.getMessage(), exception);
				break;
			default:
				break;
			}
		}
		if (dae == null) {
			throw ex;
		}
		return dae;
	}

}
