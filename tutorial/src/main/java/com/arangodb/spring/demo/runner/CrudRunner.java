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

import com.arangodb.model.TtlIndexOptions;
import com.arangodb.spring.demo.entity.Character;
import com.arangodb.spring.demo.repository.CharacterRepository;
import com.arangodb.springframework.core.ArangoOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@Component
public class CrudRunner implements CommandLineRunner {

    @Autowired
    private ArangoOperations operations;

    @Autowired
    private CharacterRepository repository;

    @Value("${arangodb.ddl.enabled:false}")
    private boolean isDdlEnabled;

    @Override
    public void run(String... args) {
        if (isDdlEnabled) {
            operations.dropDatabase();
            System.out.println("Database dropped for fresh DDL operations.");
            createTTLIndex();

            System.out.println("Schema validation passed.");

            // Set document expiration for 2 minutes from now
            Long expiresAt = Instant.now().plus(2, ChronoUnit.MINUTES).getEpochSecond();
            Character nedStark = new Character(null, "John", "Williams", expiresAt);
            Character saved = repository.save(nedStark);

            System.out.println("Saved character: " + saved);
            System.out.println("Document expires at: " + new Date(saved.expiresAt() * 1000));
            System.out.println("Current time: " + new Date());

            assertThat(saved.id()).isNotNull();
            assertThat(saved.name()).isEqualTo(nedStark.name());
            assertThat(saved.surname()).isEqualTo(nedStark.surname());
        } else {
            System.out.println("DDL operations are disabled. Skipping schema creation.");
            System.out.println(
                    "You can enable DDL operations by setting 'arangodb.ddl.enabled=true' in application.properties.");
        }
    }
    private void createTTLIndex() {
        System.out.println("Creating TTL index...");
        operations.collection(Character.class).ensureTtlIndex(
                Collections.singletonList("expiresAt"),
                new TtlIndexOptions().expireAfter(0));
        System.out.println(" TTL index created on 'expiresAt'.");
    }
}