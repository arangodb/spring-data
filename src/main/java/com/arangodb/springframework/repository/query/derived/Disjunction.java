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

package com.arangodb.springframework.repository.query.derived;

import java.util.Collection;

/**
 * Created by F625633 on 24/07/2017.
 */
public class Disjunction {

	private final String array;
	private final String predicate;
	private final Collection<Class<?>> with;

	public Disjunction(final String array, final String predicate, final Collection<Class<?>> with) {
		this.array = array;
		this.predicate = predicate;
		this.with = with;
	}

	public String getArray() {
		return array;
	}

	public String getPredicate() {
		return predicate;
	}

	public Collection<Class<?>> getWith() {
		return with;
	}

}
