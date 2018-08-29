/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
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

package com.arangodb.springframework.core;

import org.springframework.dao.DataAccessException;

import com.arangodb.entity.arangosearch.ArangoSearchPropertiesEntity;
import com.arangodb.model.arangosearch.ArangoSearchPropertiesOptions;

/**
 * @author Mark Vollmary
 *
 */
public interface ArangoSearchOperations {

	/**
	 * Return the view name
	 * 
	 * @return view name
	 */
	String name();

	/**
	 * Deletes the view from the database.
	 * 
	 * @throws DataAccessException
	 */
	void drop() throws DataAccessException;

	/**
	 * Reads the properties of the specified view.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Views/Getting.html#read-properties-of-a-view">API
	 *      Documentation</a>
	 * @return properties of the view
	 * @throws DataAccessException
	 */
	ArangoSearchPropertiesEntity getProperties() throws DataAccessException;

	/**
	 * Partially changes properties of the view.
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Views/ArangoSearch.html#partially-changes-properties-of-an-arangosearch-view">API
	 *      Documentation</a>
	 * @param options
	 *            properties to change
	 * @return properties of the view
	 * @throws DataAccessException
	 */
	ArangoSearchPropertiesEntity updateProperties(ArangoSearchPropertiesOptions options) throws DataAccessException;

	/**
	 * Changes properties of the view.
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Views/ArangoSearch.html#change-properties-of-an-arangosearch-view">API
	 *      Documentation</a>
	 * @param options
	 *            properties to change
	 * @return properties of the view
	 * @throws DataAccessException
	 */
	ArangoSearchPropertiesEntity replaceProperties(ArangoSearchPropertiesOptions options) throws DataAccessException;

}
