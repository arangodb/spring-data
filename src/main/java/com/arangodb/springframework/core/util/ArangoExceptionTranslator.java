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

import com.arangodb.springframework.core.DocumentNotFoundException;
import org.springframework.dao.*;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import com.arangodb.ArangoDBException;
import com.arangodb.springframework.ArangoUncategorizedException;
import org.springframework.lang.Nullable;

import static com.arangodb.springframework.core.util.ArangoErrors.*;

/**
 * Translate any {@link ArangoDBException} to the appropriate {@link DataAccessException} using response code and error number.
 *
 * @author Mark Vollmary
 * @author Christian Lechner
 * @author Arne Burmeister
 */
public class ArangoExceptionTranslator implements PersistenceExceptionTranslator {

    @Override
    @Nullable
    public DataAccessException translateExceptionIfPossible(final RuntimeException ex) {
        if (ex instanceof DataAccessException exception) {
            return exception;
        }

        if (ex instanceof ArangoDBException e) {
            final Integer responseCode = e.getResponseCode();
            if (responseCode == null) {
                return new ArangoUncategorizedException(e.getMessage(), e);
            }
            return switch (responseCode) {
                case ERROR_HTTP_UNAUTHORIZED, ERROR_HTTP_FORBIDDEN ->
                        new PermissionDeniedDataAccessException(e.getMessage(), e);
                case ERROR_HTTP_BAD_PARAMETER, ERROR_HTTP_METHOD_NOT_ALLOWED ->
                        new InvalidDataAccessApiUsageException(e.getMessage(), e);
                case ERROR_HTTP_NOT_FOUND -> hasErrorNumber(e, ERROR_ARANGO_DOCUMENT_NOT_FOUND) ?
                        new DocumentNotFoundException(e.getMessage(), e) :
                        new InvalidDataAccessResourceUsageException(e.getMessage(), e);
                case ERROR_HTTP_CONFLICT ->
                        hasErrorNumber(e, ERROR_ARANGO_CONFLICT) && errorMessageContains(e, "write-write") ?
                                new TransientDataAccessResourceException(e.getMessage(), e) :
                                // from AQL: {"code":409,"error":true,"errorMessage":"AQL: conflict, _rev values do not match (while executing)","errorNum":1200}
                                hasErrorNumber(e, ERROR_ARANGO_CONFLICT) && errorMessageContains(e, "_rev") ?
                                        new OptimisticLockingFailureException(e.getMessage(), e) :
                                        hasErrorNumber(e, ERROR_ARANGO_UNIQUE_CONSTRAINT_VIOLATED) && errorMessageContains(e, "_key") ?
                                                new DuplicateKeyException(e.getMessage(), e) :
                                                new DataIntegrityViolationException(e.getMessage(), e);
                case ERROR_HTTP_PRECONDITION_FAILED ->
                    // from document API: {"error":true,"code":412,"errorNum":1200,"errorMessage":"conflict, _rev values do not match"}
                        hasErrorNumber(e, ERROR_ARANGO_CONFLICT) && errorMessageContains(e, "_rev") ?
                                new OptimisticLockingFailureException(e.getMessage(), e) :
                                new DataAccessResourceFailureException(e.getMessage(), e);
                case ERROR_HTTP_SERVICE_UNAVAILABLE -> new DataAccessResourceFailureException(e.getMessage(), e);
                default -> new ArangoUncategorizedException(e.getMessage(), e);
            };
        }

        return null;
    }

    private static boolean hasErrorNumber(ArangoDBException exception, int expected) {
        return exception.getErrorNum() != null && exception.getErrorNum() == expected;
    }

    private static boolean errorMessageContains(ArangoDBException exception, String expected) {
        return exception.getErrorMessage() != null && exception.getErrorMessage().contains(expected);
    }
}
