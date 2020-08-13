/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
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

package com.arangodb.springframework.repository;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.annotation.Document;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michele Rastelli
 */
public class ShardedCollectionRepositoryTest extends AbstractArangoTest {

    @Autowired
    ShardedRepository shardedRepository;

    @Test
    public void save() {
        ShardedUser d1 = shardedRepository.save(new ShardedUser(null, "name1", "country1"));
        d1.name = "name2";
        ShardedUser d2 = shardedRepository.save(d1);
        assertThat(d2.key).isEqualTo(d1.key);
        assertThat(d2.country).isEqualTo("country1");
        assertThat(d2.name).isEqualTo("name2");

        ShardedUser d3 = shardedRepository.save(new ShardedUser(d1.key, "name3", "country1"));
        assertThat(d3.key).isEqualTo(d1.key);
        assertThat(d3.country).isEqualTo("country1");
        assertThat(d3.name).isEqualTo("name3");
    }
}

@Document(shardKeys = "country", numberOfShards = 10)
class ShardedUser {
    @Id
    String key;
    String name;
    String country;

    public ShardedUser(String key, String name, String country) {
        this.key = key;
        this.name = name;
        this.country = country;
    }

}

interface ShardedRepository extends ArangoRepository<ShardedUser, String> {
}
