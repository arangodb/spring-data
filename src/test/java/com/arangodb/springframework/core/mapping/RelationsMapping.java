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

package com.arangodb.springframework.core.mapping;

import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.core.mapping.testdata.BasicEdgeTestEntity;
import com.arangodb.springframework.core.mapping.testdata.BasicTestEntity;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Mark Vollmary
 */
abstract class RelationsMapping extends AbstractMappingTxTestAbstract {

    RelationsMapping(boolean withinTx) {
        super(withinTx, RelationsMapping.class.getDeclaredClasses());
    }

    public static class RelationsTestEntity extends BasicTestEntity {
        @Relations(edges = BasicEdgeTestEntity.class)
        private Collection<BasicTestEntity> entities;
    }

    @Test
    public void relations() {
        final BasicTestEntity e1 = new BasicTestEntity();
        template.insert(e1, insertOpts);
        final BasicTestEntity e2 = new BasicTestEntity();
        template.insert(e2, insertOpts);
        final RelationsTestEntity e0 = new RelationsTestEntity();
        template.insert(e0, insertOpts);
        template.insert(new BasicEdgeTestEntity(e0, e1), insertOpts);
        template.insert(new BasicEdgeTestEntity(e0, e2), insertOpts);

        final RelationsTestEntity document = template.find(e0.id, RelationsTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.entities, is(notNullValue()));
        assertThat(document.entities.size(), is(2));
        for (final BasicTestEntity e : document.entities) {
            assertThat(e, instanceOf(BasicTestEntity.class));
            assertThat(e.getId(), is(notNullValue()));
            assertThat(e.getId(), is(oneOf(e1.getId(), e2.getId())));
        }
    }

    public static class RelationsLazyTestEntity extends BasicTestEntity {
        @Relations(edges = BasicEdgeTestEntity.class, lazy = true)
        private Collection<BasicTestEntity> entities;
    }

    @Test
    public void relationsLazy() {
        final BasicTestEntity e1 = new BasicTestEntity();
        template.insert(e1, insertOpts);
        final BasicTestEntity e2 = new BasicTestEntity();
        template.insert(e2, insertOpts);
        final RelationsLazyTestEntity e0 = new RelationsLazyTestEntity();
        template.insert(e0, insertOpts);
        template.insert(new BasicEdgeTestEntity(e0, e1), insertOpts);
        template.insert(new BasicEdgeTestEntity(e0, e2), insertOpts);

        final RelationsLazyTestEntity document = template.find(e0.id, RelationsLazyTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.entities, is(notNullValue()));
        assertThat(document.entities.size(), is(2));
        for (final BasicTestEntity e : document.entities) {
            assertThat(e, instanceOf(BasicTestEntity.class));
            assertThat(e.getId(), is(notNullValue()));
            assertThat(e.getId(), is(oneOf(e1.getId(), e2.getId())));
        }
    }

    public static class RelationsLazySetTestEntity extends BasicTestEntity {
        @Relations(edges = BasicEdgeTestEntity.class, lazy = true)
        private Set<BasicTestEntity> entities;
    }

    @Test
    public void relationsLazySet() {
        final BasicTestEntity e1 = new BasicTestEntity();
        template.insert(e1, insertOpts);
        final BasicTestEntity e2 = new BasicTestEntity();
        template.insert(e2, insertOpts);
        final RelationsLazySetTestEntity e0 = new RelationsLazySetTestEntity();
        template.insert(e0, insertOpts);
        template.insert(new BasicEdgeTestEntity(e0, e1), insertOpts);
        template.insert(new BasicEdgeTestEntity(e0, e2), insertOpts);

        final RelationsLazySetTestEntity document = template.find(e0.id, RelationsLazySetTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.entities, is(notNullValue()));
        assertThat(document.entities.size(), is(2));
        for (final BasicTestEntity e : document.entities) {
            assertThat(e, instanceOf(BasicTestEntity.class));
            assertThat(e.getId(), is(notNullValue()));
            assertThat(e.getId(), is(oneOf(e1.getId(), e2.getId())));
        }
    }

    public static class ConstructorWithRelationsParamsTestEntity extends BasicTestEntity {
        @Relations(edges = BasicEdgeTestEntity.class)
        private final Collection<BasicTestEntity> value;

        public ConstructorWithRelationsParamsTestEntity(final Collection<BasicTestEntity> value) {
            super();
            this.value = value;
        }
    }

    @Test
    public void constructorWithRelationsParams() {
        final BasicTestEntity vertex1 = new BasicTestEntity();
        final BasicTestEntity vertex2 = new BasicTestEntity();
        template.insert(vertex1, insertOpts);
        template.insert(vertex2, insertOpts);
        final ConstructorWithRelationsParamsTestEntity entity = new ConstructorWithRelationsParamsTestEntity(
                Arrays.asList(vertex1, vertex2));
        template.insert(entity, insertOpts);
        template.insert(new BasicEdgeTestEntity(entity, vertex1), insertOpts);
        template.insert(new BasicEdgeTestEntity(entity, vertex2), insertOpts);
        final ConstructorWithRelationsParamsTestEntity document = template
                .find(entity.id, ConstructorWithRelationsParamsTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.value.stream().map((e) -> e.id).collect(Collectors.toList()),
                hasItems(vertex1.id, vertex2.id));
    }

    public static class ConstructorWithRelationsLazyParamsTestEntity extends BasicTestEntity {
        @Relations(edges = BasicEdgeTestEntity.class, lazy = true)
        private final Collection<BasicTestEntity> value;

        public ConstructorWithRelationsLazyParamsTestEntity(final Collection<BasicTestEntity> value) {
            super();
            this.value = value;
        }
    }

    @Test
    public void constructorWithRelationsLazyParams() {
        final BasicTestEntity vertex1 = new BasicTestEntity();
        final BasicTestEntity vertex2 = new BasicTestEntity();
        template.insert(vertex1, insertOpts);
        template.insert(vertex2, insertOpts);
        final ConstructorWithRelationsLazyParamsTestEntity entity = new ConstructorWithRelationsLazyParamsTestEntity(
                Arrays.asList(vertex1, vertex2));
        template.insert(entity, insertOpts);
        template.insert(new BasicEdgeTestEntity(entity, vertex1), insertOpts);
        template.insert(new BasicEdgeTestEntity(entity, vertex2), insertOpts);
        final ConstructorWithRelationsLazyParamsTestEntity document = template
                .find(entity.id, ConstructorWithRelationsLazyParamsTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.value.stream().map((e) -> e.id).collect(Collectors.toList()),
                hasItems(vertex1.id, vertex2.id));
    }

    static class SingleRelationsTestEntity extends BasicTestEntity {
        @Relations(edges = BasicEdgeTestEntity.class)
        private BasicTestEntity value;
    }

    @Test
    public void singleRelations() {
        final BasicTestEntity vertex = new BasicTestEntity();
        template.insert(vertex, insertOpts);
        final SingleRelationsTestEntity entity = new SingleRelationsTestEntity();
        template.insert(entity, insertOpts);
        template.insert(new BasicEdgeTestEntity(entity, vertex), insertOpts);
        final SingleRelationsTestEntity document = template.find(entity.id, SingleRelationsTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.value.id, is(vertex.id));

    }

    static class SingleRelationsLazyTestEntity extends BasicTestEntity {
        @Relations(edges = BasicEdgeTestEntity.class, lazy = true)
        private BasicTestEntity value;
    }

    @Test
    public void singleRelationsLazy() {
        final BasicTestEntity vertex = new BasicTestEntity();
        template.insert(vertex, insertOpts);
        final SingleRelationsLazyTestEntity entity = new SingleRelationsLazyTestEntity();
        template.insert(entity, insertOpts);
        template.insert(new BasicEdgeTestEntity(entity, vertex), insertOpts);
        final SingleRelationsLazyTestEntity document = template.find(entity.id, SingleRelationsLazyTestEntity.class, findOpts)
                .get();
        assertThat(document, is(notNullValue()));
        assertThat(document.value.getId(), is(vertex.id));

    }

    static class AnotherBasicEdgeTestEntity extends BasicEdgeTestEntity {
        public AnotherBasicEdgeTestEntity() {
            super();
        }

        public AnotherBasicEdgeTestEntity(final BasicTestEntity from, final BasicTestEntity to) {
            super(from, to);
        }
    }

    static class MultipleEdgeTypeRelationsTestEntity extends BasicTestEntity {
        @Relations(edges = {BasicEdgeTestEntity.class, AnotherBasicEdgeTestEntity.class}, maxDepth = 2)
        private Collection<BasicTestEntity> entities;
    }

    @Test
    public void multipleEdgeTypeRelations() {
        final BasicTestEntity e1 = new BasicTestEntity();
        template.insert(e1, insertOpts);
        final BasicTestEntity e2 = new BasicTestEntity();
        template.insert(e2, insertOpts);
        final MultipleEdgeTypeRelationsTestEntity e0 = new MultipleEdgeTypeRelationsTestEntity();
        template.insert(e0, insertOpts);
        template.insert(new BasicEdgeTestEntity(e0, e1), insertOpts);
        template.insert(new AnotherBasicEdgeTestEntity(e1, e2), insertOpts);

        final MultipleEdgeTypeRelationsTestEntity document = template
                .find(e0.id, MultipleEdgeTypeRelationsTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.entities, is(notNullValue()));
        assertThat(document.entities.size(), is(2));
        for (final BasicTestEntity e : document.entities) {
            assertThat(e, instanceOf(BasicTestEntity.class));
            assertThat(e.getId(), is(notNullValue()));
            assertThat(e.getId(), is(oneOf(e1.getId(), e2.getId())));
        }
    }
}
