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

package com.arangodb.springframework.core.convert;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Mark Vollmary
 *
 */
@SuppressWarnings("deprecation")
public class DBDocumentEntity extends HashMap<String, Object> implements DBEntity {

	private static final long serialVersionUID = -7251842887063588024L;

	public DBDocumentEntity() {
		super();
	}

	public DBDocumentEntity(final Map<? extends String, ? extends Object> m) {
		super(m);
	}

	/**
	 * @deprecated Will be removed in 3.0.0
	 */
	@Deprecated
	@Override
	public boolean add(final Object value) {
		throw new UnsupportedOperationException();
	}

}
