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

package com.arangodb.springframework.example.polymorphic.template;

import com.arangodb.ArangoCursor;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentDeleteEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import com.arangodb.entity.MultiDocumentEntity;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentDeleteOptions;
import com.arangodb.model.DocumentUpdateOptions;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.example.polymorphic.entity.Animal;
import com.arangodb.springframework.example.polymorphic.entity.Dog;
import com.arangodb.springframework.example.polymorphic.entity.Eagle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Michele Rastelli
 */
public class PolymorphicTemplate extends AbstractArangoTest {

    @Test
    public void query() {
        Dog dog = new Dog();
        dog.setId("1");
        dog.setName("dog");
        dog.setTeeths(11);

        Eagle eagle = new Eagle();
        dog.setId("2");
        eagle.setName("eagle");
        eagle.setWingspan(2.5);

        template.insert(dog);
        template.insert(eagle);

        final ArangoCursor<Animal> cursor = template.query("FOR a IN animals RETURN a", Animal.class);
        assertThat(cursor, is(notNullValue()));
        final List<Animal> animals = cursor.asListRemaining();
        assertThat(animals.size(), is(2));
        assertThat(animals.stream().anyMatch(it -> it.equals(eagle)), is(true));
        assertThat(animals.stream().anyMatch(it -> it.equals(dog)), is(true));
    }

    @Test
    public void insertVariance() {
        Dog dog = new Dog();
        dog.setId("1");
        dog.setName("dog");
        dog.setTeeths(11);

        DocumentCreateEntity<Animal> res = template.insert(dog, new DocumentCreateOptions().returnNew(true));
        assertThat(res.getNew(), is(dog));
    }

    @Test
    public void insertAllVariance() {
        Dog dog1 = new Dog();
        dog1.setId("1");
        dog1.setName("dog1");
        dog1.setTeeths(11);

        Dog dog2 = new Dog();
        dog1.setId("2");
        dog1.setName("dog2");
        dog1.setTeeths(22);

        List<Dog> dogs = List.of(dog1, dog2);

        MultiDocumentEntity<DocumentCreateEntity<Animal>> res =
                template.insertAll(dogs, new DocumentCreateOptions().returnNew(true), Animal.class);
        assertThat(res.getDocuments().stream().map(DocumentCreateEntity::getNew).toList().containsAll(dogs), is(true));
    }

    @Test
    public void deleteVariance() {
        Dog dog = new Dog();
        dog.setId("1");
        dog.setName("dog");
        dog.setTeeths(11);

        template.insert(dog);
        DocumentDeleteEntity<Animal> res = template.delete(dog.getId(), new DocumentDeleteOptions().returnOld(true), Animal.class);
        assertThat(res.getOld(), is(dog));
    }

    @Test
    public void deleteAllVariance() {
        Dog dog1 = new Dog();
        dog1.setId("1");
        dog1.setName("dog1");
        dog1.setTeeths(11);

        Dog dog2 = new Dog();
        dog1.setId("2");
        dog1.setName("dog2");
        dog1.setTeeths(22);

        List<Dog> dogs = List.of(dog1, dog2);
        template.insertAll(dogs, Animal.class);
        MultiDocumentEntity<DocumentDeleteEntity<Animal>> res = template.deleteAll(List.of(dog1.getId(), dog2.getId()), new DocumentDeleteOptions().returnOld(true), Animal.class);
        assertThat(res.getDocuments().stream().map(DocumentDeleteEntity::getOld).toList().containsAll(dogs), is(true));
    }

    @Test
    public void updateVariance() {
        Dog dog = new Dog();
        dog.setId("1");
        dog.setName("dog");
        dog.setTeeths(11);

        template.insert(dog);
        DocumentUpdateEntity<Animal> res = template.update(dog.getId(), dog, new DocumentUpdateOptions().returnNew(true));
        assertThat(res.getNew(), is(dog));
    }

    @Test
    public void updateAllVariance() {
        Dog dog1 = new Dog();
        dog1.setId("1");
        dog1.setName("dog1");
        dog1.setTeeths(11);

        Dog dog2 = new Dog();
        dog1.setId("2");
        dog1.setName("dog2");
        dog1.setTeeths(22);

        List<Dog> dogs = List.of(dog1, dog2);
        template.insertAll(dogs, Animal.class);
        MultiDocumentEntity<DocumentUpdateEntity<Animal>> res = template.updateAll(dogs, new DocumentUpdateOptions().returnNew(true), Animal.class);
        assertThat(res.getDocuments().stream().map(DocumentUpdateEntity::getNew).toList().containsAll(dogs), is(true));
    }

}
