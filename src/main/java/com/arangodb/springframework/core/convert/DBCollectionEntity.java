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

import java.util.ArrayList;
import java.util.Collection;

/**
 * @deprecated Will be removed in 3.0.0
 * 
 * @author Mark Vollmary
 */
@Deprecated
public class DBCollectionEntity extends ArrayList<Object> implements DBEntity {

	private static final long serialVersionUID = -2068955559598596722L;

	public DBCollectionEntity() {
		super();
	}

	public DBCollectionEntity(final Collection<? extends Object> c) {
		super(c);
	}

	@Override
	public Object put(final String key, final Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(final Object key) {
		throw new UnsupportedOperationException();
	}

}
