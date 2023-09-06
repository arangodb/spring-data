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

/**
 * @see <a href="https://github.com/arangodb/arangodb/blob/devel/lib/Basics/errors.dat">General ArangoDB storage errors</a>
 * @author Mark Vollmary
 * @author Christian Lechner
 * @author Arne Burmeister
 */
public class ArangoErrors {

	/**
	 * Bad parameter, will be raised when the HTTP request does not fulfill the requirements.
	 */
	public static final int ERROR_HTTP_BAD_PARAMETER = 400;

	/**
	 * Unauthorized, will be raised when authorization is required but the user is not authorized.
	 */
	public static final int ERROR_HTTP_UNAUTHORIZED = 401;

	/**
	 * Forbidden, will be raised when the operation is forbidden.
	 */
	public static final int ERROR_HTTP_FORBIDDEN = 403;

	/**
	 * Not found, will be raised when an URI is unknown.
	 */
	public static final int ERROR_HTTP_NOT_FOUND = 404;

	/**
	 * Method not supported, will be raised when an unsupported HTTP method is used for an operation.
	 */
	public static final int ERROR_HTTP_METHOD_NOT_ALLOWED = 405;

	/**
	 * Conflict, will be raised when a conflict is encountered.
	 */
	public static final int ERROR_HTTP_CONFLICT = 409;

	/**
	 * Precondition failed, will be raised when a precondition for an HTTP request is not met.
	 */
	public static final int ERROR_HTTP_PRECONDITION_FAILED = 412;

	/**
	 * Internal server error, will be raised when an internal server is encountered.
	 */
	public static final int ERROR_HTTP_SERVER_ERROR = 500;

	/**
	 * Service unavailable, will be raised when a service is temporarily unavailable.
	 */
	public static final int ERROR_HTTP_SERVICE_UNAVAILABLE = 503;

	/**
	 * Conflict, will be raised when updating or deleting a document and a conflict has been detected.
	 */
	public static final int ERROR_ARANGO_CONFLICT = 1200;

	/**
	 * Document not found, will be raised when a document with a given identifier is unknown.
	 */
	public static final int ERROR_ARANGO_DOCUMENT_NOT_FOUND = 1202;

	/**
	 * Unique constraint violated, will be raised when there is a unique constraint violation.
	 */
	public static final int ERROR_ARANGO_UNIQUE_CONSTRAINT_VIOLATED = 1210;

}
