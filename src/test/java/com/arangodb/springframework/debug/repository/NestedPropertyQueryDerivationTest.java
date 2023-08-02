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

package com.arangodb.springframework.debug.repository;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.debug.repository.entity.User;
import com.arangodb.springframework.debug.repository.entity.UserLogin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michele Rastelli
 */
public class NestedPropertyQueryDerivationTest extends AbstractArangoTest {

	@Autowired
	private UserRepository repo;

	@Test
	public void save() {
		User u = new User("id", new UserLogin("email@email.com", "pass"));
		repo.save(u);
		boolean existsByLoginEmail = repo.existsByLoginEmailIgnoreCase("email@email.com");
		assertThat(existsByLoginEmail).isTrue();
	}
}
