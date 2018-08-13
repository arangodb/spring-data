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

package com.arangodb.springframework;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;

import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.testdata.Person;

/**
 * @author Mark Vollmary
 *
 */
public class AuditorProvider implements AuditorAware<Person> {

	private final ArangoOperations operations;
	private final Person person;

	public AuditorProvider(final ArangoOperations operations) {
		super();
		this.operations = operations;
		person = new Person();
		person.setId("auditor");
		person.setName("Auditor");
	}

	@Override
	public Optional<Person> getCurrentAuditor() {
		operations.repsert(person);
		return Optional.of(person);
	}

}
