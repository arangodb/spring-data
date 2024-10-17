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

package com.arangodb.spring.demo.runner;

import com.arangodb.spring.demo.entity.Character;
import com.arangodb.spring.demo.repository.CharacterRepository;
import com.arangodb.springframework.core.ArangoOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.ComponentScan;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentScan("com.arangodb.spring.demo")
public class CrudRunner implements CommandLineRunner {

    @Autowired
    private ArangoOperations operations;

    @Autowired
    private CharacterRepository repository;

    @Override
    public void run(String... args) {
        // first drop the database so that we can run this multiple times with the same dataset
        operations.dropDatabase();

        System.out.println("# CRUD operations");

        // save a single entity in the database
        // there is no need of creating the collection first. This happen automatically
        Character nedStark = new Character(null, "Ned", "Stark");
        Character saved = repository.save(nedStark);
        System.out.println("Ned Stark saved in the database: " + saved);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.name()).isEqualTo(nedStark.name());
        assertThat(saved.surname()).isEqualTo(nedStark.surname());
    }
}
