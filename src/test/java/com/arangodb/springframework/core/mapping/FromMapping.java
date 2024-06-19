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

import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.core.mapping.testdata.BasicEdgeLazyTestEntity;
import com.arangodb.springframework.core.mapping.testdata.BasicEdgeTestEntity;
import com.arangodb.springframework.core.mapping.testdata.BasicTestEntity;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Mark Vollmary
 */
abstract class FromMapping extends AbstractMappingTxTestAbstract {

    FromMapping(boolean withinTx) {
        super(withinTx, FromMapping.class.getDeclaredClasses());
    }

    public static class DocumentFromTestEntity extends BasicTestEntity {
        @From
        private Collection<BasicEdgeLazyTestEntity> entities;
    }

    @Test
    public void documentFrom() {
        final DocumentFromTestEntity e0 = new DocumentFromTestEntity();
        template.insert(e0, insertOpts);
        final DocumentFromTestEntity e1 = new DocumentFromTestEntity();
        template.insert(e1, insertOpts);
        final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e0, e1);
        template.insert(edge0, insertOpts);
        final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e0, e1);
        template.insert(edge1, insertOpts);
        final DocumentFromTestEntity document = template.find(e0.id, DocumentFromTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.entities, is(notNullValue()));
        assertThat(document.entities.size(), is(2));
        for (final BasicEdgeLazyTestEntity e : document.entities) {
            assertThat(e, instanceOf(BasicEdgeLazyTestEntity.class));
            assertThat(e.getId(), is(notNullValue()));
            assertThat(e.getId(), is(oneOf(edge0.getId(), edge1.getId())));
            assertThat(e.getFrom(), is(notNullValue()));
            assertThat(e.getFrom().getId(), is(notNullValue()));
            assertThat(e.getFrom().getId(), is(e0.getId()));
        }
    }

    public static class DocumentFromLazyTestEntity extends BasicTestEntity {
        @From(lazy = true)
        private Collection<BasicEdgeLazyTestEntity> entities;
    }

    @Test
    public void documentFromLazy() {
        final DocumentFromLazyTestEntity e0 = new DocumentFromLazyTestEntity();
        template.insert(e0, insertOpts);
        final DocumentFromLazyTestEntity e1 = new DocumentFromLazyTestEntity();
        template.insert(e1, insertOpts);
        final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e0, e1);
        template.insert(edge0, insertOpts);
        final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e0, e1);
        template.insert(edge1, insertOpts);
        final DocumentFromLazyTestEntity document = template.find(e0.id, DocumentFromLazyTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.entities, is(notNullValue()));
        assertThat(document.entities.size(), is(2));
        for (final BasicEdgeLazyTestEntity e : document.entities) {
            assertThat(e, instanceOf(BasicEdgeLazyTestEntity.class));
            assertThat(e.getId(), is(notNullValue()));
            assertThat(e.getId(), is(oneOf(edge0.getId(), edge1.getId())));
            assertThat(e.getFrom(), is(notNullValue()));
            assertThat(e.getFrom().getId(), is(notNullValue()));
            assertThat(e.getFrom().getId(), is(e0.getId()));
        }
    }

    public static class DocumentFromLazySetTestEntity extends BasicTestEntity {
        @From(lazy = true)
        private Set<BasicEdgeLazyTestEntity> entities;
    }

    @Test
    public void documentFromLazySet() {
        final DocumentFromLazySetTestEntity e0 = new DocumentFromLazySetTestEntity();
        template.insert(e0, insertOpts);
        final DocumentFromLazySetTestEntity e1 = new DocumentFromLazySetTestEntity();
        template.insert(e1, insertOpts);
        final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e0, e1);
        template.insert(edge0, insertOpts);
        final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(e0, e1);
        template.insert(edge1, insertOpts);
        final DocumentFromLazySetTestEntity document = template.find(e0.id, DocumentFromLazySetTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.entities, is(notNullValue()));
        assertThat(document.entities.size(), is(2));
        for (final BasicEdgeLazyTestEntity e : document.entities) {
            assertThat(e, instanceOf(BasicEdgeLazyTestEntity.class));
            assertThat(e.getId(), is(notNullValue()));
            assertThat(e.getId(), is(oneOf(edge0.getId(), edge1.getId())));
            assertThat(e.getFrom(), is(notNullValue()));
            assertThat(e.getFrom().getId(), is(notNullValue()));
            assertThat(e.getFrom().getId(), is(e0.getId()));
        }
    }

    public static class ConstructorWithFromParamsTestEntity extends BasicTestEntity {
        @From
        private final Collection<BasicEdgeLazyTestEntity> value;

        public ConstructorWithFromParamsTestEntity(final Collection<BasicEdgeLazyTestEntity> value) {
            super();
            this.value = value;
        }
    }

    @Test
    public void constructorWithFromParams() {
        final ConstructorWithFromParamsTestEntity entity = new ConstructorWithFromParamsTestEntity(null);
        template.insert(entity, insertOpts);
        final BasicTestEntity to = new BasicTestEntity();
        template.insert(to, insertOpts);
        final BasicEdgeLazyTestEntity edge1 = new BasicEdgeLazyTestEntity(entity, to);
        final BasicEdgeLazyTestEntity edge2 = new BasicEdgeLazyTestEntity(entity, to);
        template.insert(edge1, insertOpts);
        template.insert(edge2, insertOpts);
        final ConstructorWithFromParamsTestEntity document = template
                .find(entity.id, ConstructorWithFromParamsTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.value.stream().map((e) -> e.id).collect(Collectors.toList()), hasItems(edge1.id, edge2.id));
    }

    public static class ConstructorWithFromLazyParamsTestEntity extends BasicTestEntity {
        @From(lazy = true)
        private final Collection<BasicEdgeTestEntity> value;

        public ConstructorWithFromLazyParamsTestEntity(final Collection<BasicEdgeTestEntity> value) {
            super();
            this.value = value;
        }
    }

    @Test
    public void constructorWithFromLazyParams() {
        final ConstructorWithFromLazyParamsTestEntity entity = new ConstructorWithFromLazyParamsTestEntity(null);
        template.insert(entity, insertOpts);
        final BasicTestEntity to = new BasicTestEntity();
        template.insert(to, insertOpts);
        final BasicEdgeTestEntity edge1 = new BasicEdgeTestEntity(entity, to);
        final BasicEdgeTestEntity edge2 = new BasicEdgeTestEntity(entity, to);
        template.insert(edge1, insertOpts);
        template.insert(edge2, insertOpts);
        final ConstructorWithFromLazyParamsTestEntity document = template
                .find(entity.id, ConstructorWithFromLazyParamsTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.value.stream().map((e) -> e.getId()).collect(Collectors.toList()),
                hasItems(edge1.id, edge2.id));
    }

    public static class SingleDocumentFromTestEntity extends BasicTestEntity {
        @From
        private BasicEdgeLazyTestEntity entity;
    }

    @Test
    public void singleDocumentFrom() {
        final SingleDocumentFromTestEntity e0 = new SingleDocumentFromTestEntity();
        template.insert(e0, insertOpts);
        final SingleDocumentFromTestEntity e1 = new SingleDocumentFromTestEntity();
        template.insert(e1, insertOpts);
        final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e0, e1);
        template.insert(edge0, insertOpts);
        final SingleDocumentFromTestEntity document = template.find(e0.id, SingleDocumentFromTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.entity, is(notNullValue()));
        assertThat(document.entity.getId(), is(edge0.id));
        assertThat(document.entity.getFrom(), is(notNullValue()));
        assertThat(document.entity.getFrom().getId(), is(notNullValue()));
        assertThat(document.entity.getFrom().getId(), is(e0.getId()));
    }

    public static class SingleDocumentFromLazyTestEntity extends BasicTestEntity {
        @From(lazy = true)
        private BasicEdgeLazyTestEntity entity;
    }

    @Test
    public void singleDocumentFromLazy() {
        final SingleDocumentFromLazyTestEntity e0 = new SingleDocumentFromLazyTestEntity();
        template.insert(e0, insertOpts);
        final SingleDocumentFromLazyTestEntity e1 = new SingleDocumentFromLazyTestEntity();
        template.insert(e1, insertOpts);
        final BasicEdgeLazyTestEntity edge0 = new BasicEdgeLazyTestEntity(e0, e1);
        template.insert(edge0, insertOpts);
        final SingleDocumentFromLazyTestEntity document = template.find(e0.id, SingleDocumentFromLazyTestEntity.class, findOpts)
                .get();
        assertThat(document, is(notNullValue()));
        assertThat(document.entity, is(notNullValue()));
        assertThat(document.entity.getId(), is(edge0.id));
        assertThat(document.entity.getFrom(), is(notNullValue()));
        assertThat(document.entity.getFrom().getId(), is(notNullValue()));
        assertThat(document.entity.getFrom().getId(), is(e0.getId()));
    }
}
