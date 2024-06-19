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

import com.arangodb.springframework.annotation.ArangoId;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.core.mapping.testdata.BasicTestEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

abstract class RefMapping extends AbstractMappingTxTestAbstract {

    RefMapping(boolean withinTx) {
        super(withinTx, RefMapping.class.getDeclaredClasses());
    }

    public static class SingleReferenceTestEntity extends BasicTestEntity {
        @Ref
        private BasicTestEntity entity;
    }

    @Test
    public void singleRef() {
        final BasicTestEntity e1 = new BasicTestEntity();
        template.insert(e1, insertOpts);
        final SingleReferenceTestEntity e0 = new SingleReferenceTestEntity();
        e0.entity = e1;
        template.insert(e0, insertOpts);
        final SingleReferenceTestEntity document = template.find(e0.id, SingleReferenceTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.entity, is(notNullValue()));
        assertThat(document.entity.id, is(e1.id));
    }

    public static class SingleReferenceLazyTestEntity extends BasicTestEntity {
        @Ref(lazy = true)
        private BasicTestEntity entity;

        public BasicTestEntity getEntity() {
            return entity;
        }
    }

    @Test
    public void singleRefLazy() {
        final BasicTestEntity e1 = new BasicTestEntity();
        template.insert(e1, insertOpts);
        final SingleReferenceLazyTestEntity e0 = new SingleReferenceLazyTestEntity();
        e0.entity = e1;
        template.insert(e0, insertOpts);
        final SingleReferenceLazyTestEntity document = template.find(e0.id, SingleReferenceLazyTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.entity, is(notNullValue()));
        assertThat(document.entity, instanceOf(BasicTestEntity.class));
        assertThat(document.entity.getId(), is(e1.getId()));
    }

    public static class DeepReferenceTestEntity extends BasicTestEntity {
        @Ref
        private SingleReferenceTestEntity entity;
    }

    @Test
    public void deepRef() {
        BasicTestEntity e1 = new BasicTestEntity();
        template.insert(e1, insertOpts);

        SingleReferenceTestEntity e2 = new SingleReferenceTestEntity();
        e2.entity = e1;
        template.insert(e2, insertOpts);

        DeepReferenceTestEntity e3 = new DeepReferenceTestEntity();
        e3.entity = e2;
        template.insert(e3, insertOpts);

        final DeepReferenceTestEntity document = template.find(e3.id, DeepReferenceTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.entity, is(notNullValue()));
        assertThat(document.entity.id, is(e2.id));
        assertThat(document.entity.entity.id, is(e1.id));
    }

    public static class DeepReferenceLazyTestEntity extends BasicTestEntity {
        @Ref(lazy = true)
        private SingleReferenceLazyTestEntity entity;

        public SingleReferenceLazyTestEntity getEntity() {
            return entity;
        }
    }

    @Test
    public void deepRefLazy() {
        BasicTestEntity e1 = new BasicTestEntity();
        template.insert(e1, insertOpts);

        SingleReferenceLazyTestEntity e2 = new SingleReferenceLazyTestEntity();
        e2.entity = e1;
        template.insert(e2, insertOpts);

        DeepReferenceLazyTestEntity e3 = new DeepReferenceLazyTestEntity();
        e3.entity = e2;
        template.insert(e3, insertOpts);

        final DeepReferenceLazyTestEntity document = template.find(e3.id, DeepReferenceLazyTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.getEntity(), is(notNullValue()));
        assertThat(document.getEntity().getId(), is(e2.id));
        assertThat(document.getEntity().getEntity().getId(), is(e1.id));
    }

    public static class MultiReferenceTestEntity extends BasicTestEntity {
        @Ref
        private Collection<BasicTestEntity> entities;
    }

    @Test
    public void multiRef() {
        final BasicTestEntity e1 = new BasicTestEntity();
        template.insert(e1, insertOpts);
        final BasicTestEntity e2 = new BasicTestEntity();
        template.insert(e2, insertOpts);
        final MultiReferenceTestEntity e0 = new MultiReferenceTestEntity();
        e0.entities = Arrays.asList(e1, e2);
        template.insert(e0, insertOpts);
        final MultiReferenceTestEntity document = template.find(e0.id, MultiReferenceTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.entities, is(notNullValue()));
        assertThat(document.entities.size(), is(2));
        for (final BasicTestEntity e : document.entities) {
            assertThat(e, instanceOf(BasicTestEntity.class));
            assertThat(e.getId(), is(notNullValue()));
            assertThat(e.getId(), is(oneOf(e1.getId(), e2.getId())));
        }
    }

    public static class MultiReferenceLazyTestEntity extends BasicTestEntity {
        @Ref(lazy = true)
        private Collection<BasicTestEntity> entities;
    }

    @Test
    public void multiRefLazy() {
        final BasicTestEntity e1 = new BasicTestEntity();
        template.insert(e1, insertOpts);
        final BasicTestEntity e2 = new BasicTestEntity();
        template.insert(e2, insertOpts);
        final MultiReferenceLazyTestEntity e0 = new MultiReferenceLazyTestEntity();
        e0.entities = Arrays.asList(e1, e2);
        template.insert(e0, insertOpts);
        final MultiReferenceLazyTestEntity document = template.find(e0.id, MultiReferenceLazyTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.entities, is(notNullValue()));
        assertThat(document.entities.size(), is(2));
        for (final BasicTestEntity e : document.entities) {
            assertThat(e, instanceOf(BasicTestEntity.class));
            assertThat(e.getId(), is(notNullValue()));
            assertThat(e.getId(), is(oneOf(e1.getId(), e2.getId())));
        }
    }

    public static class NestedReferenceTestEntity extends BasicTestEntity {
        private NestedReferenceSubTestEntity sub;
    }

    public static class NestedReferenceSubTestEntity {
        @Ref
        private Collection<BasicTestEntity> entities;
    }

    @Test
    public void testNestedRef() {
        final NestedReferenceTestEntity o = new NestedReferenceTestEntity();
        o.sub = new NestedReferenceSubTestEntity();
        o.sub.entities = new ArrayList<>();
        final BasicTestEntity e = new BasicTestEntity();
        o.sub.entities.add(e);
        template.insert(e, insertOpts);
        template.insert(o, insertOpts);
        final NestedReferenceTestEntity document = template.find(o.id, NestedReferenceTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.sub, is(notNullValue()));
        assertThat(document.sub.entities, is(notNullValue()));
        assertThat(document.sub.entities.size(), is(1));
        assertThat(document.sub.entities.iterator().next().id, is(e.id));
    }

    public static class NestedReferenceLazyTestEntity extends BasicTestEntity {
        private NestedReferenceLazySubTestEntity sub;
    }

    public static class NestedReferenceLazySubTestEntity {
        @Ref(lazy = true)
        private Collection<BasicTestEntity> entities;

        public Collection<BasicTestEntity> getEntities() {
            return entities;
        }
    }

    @Test
    public void testNestedRefLazy() {
        final NestedReferenceLazyTestEntity o = new NestedReferenceLazyTestEntity();
        o.sub = new NestedReferenceLazySubTestEntity();
        o.sub.entities = new ArrayList<>();
        final BasicTestEntity e = new BasicTestEntity();
        o.sub.entities.add(e);
        template.insert(e, insertOpts);
        template.insert(o, insertOpts);
        final NestedReferenceLazyTestEntity document = template.find(o.id, NestedReferenceLazyTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.sub, is(notNullValue()));
        assertThat(document.sub.getEntities(), is(notNullValue()));
        assertThat(document.sub.getEntities().size(), is(1));
        assertThat(document.sub.getEntities().iterator().next().getId(), is(e.id));
    }

    public static class ConstructorWithRefParamsTestEntity extends BasicTestEntity {
        @Ref
        private final BasicTestEntity value1;
        @Ref
        private final Collection<BasicTestEntity> value2;

        public ConstructorWithRefParamsTestEntity(final BasicTestEntity value1,
                                                  final Collection<BasicTestEntity> value2) {
            super();
            this.value1 = value1;
            this.value2 = value2;
        }
    }

    @Test
    public void constructorWithRefParams() {
        final BasicTestEntity value1 = new BasicTestEntity();
        final BasicTestEntity value2 = new BasicTestEntity();
        final BasicTestEntity value3 = new BasicTestEntity();
        template.insert(value1, insertOpts);
        template.insert(value2, insertOpts);
        template.insert(value3, insertOpts);
        final ConstructorWithRefParamsTestEntity entity = new ConstructorWithRefParamsTestEntity(value1,
                Arrays.asList(value2, value3));
        template.insert(entity, insertOpts);
        final ConstructorWithRefParamsTestEntity document = template
                .find(entity.id, ConstructorWithRefParamsTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.value1.id, is(value1.id));
        assertThat(document.value2.size(), is(2));
        assertThat(document.value2.stream().map((e) -> e.id).collect(Collectors.toList()),
                hasItems(value2.id, value3.id));
    }

    public static class ConstructorWithRefLazyParamsTestEntity extends BasicTestEntity {
        @Ref(lazy = true)
        private final BasicTestEntity value1;
        @Ref(lazy = true)
        private final Collection<BasicTestEntity> value2;

        public ConstructorWithRefLazyParamsTestEntity(final BasicTestEntity value1,
                                                      final Collection<BasicTestEntity> value2) {
            super();
            this.value1 = value1;
            this.value2 = value2;
        }
    }

    @Test
    public void constructorWithRefLazyParams() {
        final BasicTestEntity value1 = new BasicTestEntity();
        final BasicTestEntity value2 = new BasicTestEntity();
        final BasicTestEntity value3 = new BasicTestEntity();
        template.insert(value1, insertOpts);
        template.insert(value2, insertOpts);
        template.insert(value3, insertOpts);
        final ConstructorWithRefLazyParamsTestEntity entity = new ConstructorWithRefLazyParamsTestEntity(value1,
                Arrays.asList(value2, value3));
        template.insert(entity, insertOpts);
        final ConstructorWithRefLazyParamsTestEntity document = template
                .find(entity.id, ConstructorWithRefLazyParamsTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.value1.getId(), is(value1.id));
        assertThat(document.value2.size(), is(2));
        assertThat(document.value2.stream().map((e) -> e.getId()).collect(Collectors.toList()),
                hasItems(value2.id, value3.id));
    }

    public static class PropertyRefInheritanceTestEntity extends BasicTestEntity {
        @Ref
        private BasicTestEntity value;
    }

    public static class SimpleBasicChildTestEntity extends BasicTestEntity {
        private String field;
    }

    public static class ComplexBasicChildTestEntity extends BasicTestEntity {
        private BasicTestEntity nestedEntity;
    }

    @Test
    public void propertyRefInheritanceMapping() {
        final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
        innerChild.field = "value";
        final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
        child.nestedEntity = innerChild;
        final PropertyRefInheritanceTestEntity entity = new PropertyRefInheritanceTestEntity();
        entity.value = child;
        template.insert(child, insertOpts);
        template.insert(entity, insertOpts);
        final PropertyRefInheritanceTestEntity document = template
                .find(entity.getId(), PropertyRefInheritanceTestEntity.class, findOpts).get();
        assertThat(document, is(notNullValue()));
        assertThat(document.value, is(instanceOf(ComplexBasicChildTestEntity.class)));
        final ComplexBasicChildTestEntity complexDocument = (ComplexBasicChildTestEntity) document.value;
        assertThat(complexDocument.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
        final SimpleBasicChildTestEntity simpleDocument = (SimpleBasicChildTestEntity) complexDocument.nestedEntity;
        assertThat(simpleDocument.field, is(innerChild.field));
    }

    @Document("sameCollection")
    static class TwoTypesInSameCollectionA extends BasicTestEntity {
        String value;
        String a;
    }

    @Document("sameCollection")
    static class TwoTypesInSameCollectionB extends BasicTestEntity {
        String value;
        String b;
    }

    static class SameCollectionTestEntity extends BasicTestEntity {
        @Ref
        Collection<BasicTestEntity> value;
    }

    @Test
    public void twoTypesInSameCollection() {
        final TwoTypesInSameCollectionA a = new TwoTypesInSameCollectionA();
        a.value = "testA";
        a.a = "testA";
        final TwoTypesInSameCollectionB b = new TwoTypesInSameCollectionB();
        b.value = "testB";
        b.b = "testB";
        final SameCollectionTestEntity c = new SameCollectionTestEntity();
        c.value = new ArrayList<>();
        c.value.add(a);
        c.value.add(b);

        template.insert(a, insertOpts);
        template.insert(b, insertOpts);
        template.insert(c, insertOpts);
        final Optional<SameCollectionTestEntity> findC = template.find(c.getId(), SameCollectionTestEntity.class, findOpts);
        assertThat(findC.isPresent(), is(true));
        final Collection<BasicTestEntity> value = findC.get().value;
        assertThat(value.size(), is(2));
        {
            assertThat(value.stream().filter(v -> v instanceof TwoTypesInSameCollectionA).count(), is(1L));
            final Optional<BasicTestEntity> findA = value.stream().filter(v -> v instanceof TwoTypesInSameCollectionA)
                    .findFirst();
            assertThat(findA.isPresent(), is(true));
            final TwoTypesInSameCollectionA aa = (TwoTypesInSameCollectionA) findA.get();
            assertThat(aa.value, is("testA"));
            assertThat(aa.a, is("testA"));
        }
        {
            assertThat(value.stream().filter(v -> v instanceof TwoTypesInSameCollectionB).count(), is(1L));
            final Optional<BasicTestEntity> findB = value.stream().filter(v -> v instanceof TwoTypesInSameCollectionB)
                    .findFirst();
            assertThat(findB.isPresent(), is(true));
            final TwoTypesInSameCollectionB bb = (TwoTypesInSameCollectionB) findB.get();
            assertThat(bb.value, is("testB"));
            assertThat(bb.b, is("testB"));
        }
    }

    static class ArangoIdOnlyTestEntity {
        @ArangoId
        String id;
        @Ref
        Collection<ArangoIdOnlyTestEntity> refs;
    }

    @Test
    public void arangoIdOnly() {
        final ArangoIdOnlyTestEntity a = new ArangoIdOnlyTestEntity();
        final ArangoIdOnlyTestEntity b = new ArangoIdOnlyTestEntity();
        b.refs = Arrays.asList(a);

        template.insert(a, insertOpts);
        template.insert(b, insertOpts);

        final Optional<ArangoIdOnlyTestEntity> find = template.find(b.id, ArangoIdOnlyTestEntity.class, findOpts);
        assertThat(find.isPresent(), is(true));
        final Collection<ArangoIdOnlyTestEntity> refs = find.get().refs;
        assertThat(refs.size(), is(1));
        assertThat(refs.stream().findFirst().get().id, is(a.id));
    }

    static class ArangoIdAndIdTestEntity {
        @ArangoId
        String arangoId;
        @Id
        String id;
        @Ref
        Collection<ArangoIdAndIdTestEntity> refs;
    }

    @Test
    public void arangoIdAndId() {
        final ArangoIdAndIdTestEntity a = new ArangoIdAndIdTestEntity();
        final ArangoIdAndIdTestEntity b = new ArangoIdAndIdTestEntity();
        b.refs = Arrays.asList(a);

        template.insert(a, insertOpts);
        a.id = null;
        template.insert(b, insertOpts);

        final Optional<ArangoIdAndIdTestEntity> find = template.find(b.arangoId, ArangoIdAndIdTestEntity.class, findOpts);
        assertThat(find.isPresent(), is(true));
        final Collection<ArangoIdAndIdTestEntity> refs = find.get().refs;
        assertThat(refs.size(), is(1));
        assertThat(refs.stream().findFirst().get().arangoId, is(a.arangoId));
    }
}
