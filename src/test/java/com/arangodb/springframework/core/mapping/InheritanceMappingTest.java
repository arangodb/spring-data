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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.core.mapping.testdata.BasicEdgeTestEntity;
import com.arangodb.springframework.core.mapping.testdata.BasicTestEntity;

/**
 * @author Mark Vollmary
 *
 */
public class InheritanceMappingTest extends AbstractArangoTest {

	public static class SimpleBasicChildTestEntity extends BasicTestEntity {
		private String field;
	}

	public static class ComplexBasicChildTestEntity extends BasicTestEntity {
		private BasicTestEntity nestedEntity;
	}

	public static class PropertyInheritanceTestEntity extends BasicTestEntity {
		private BasicTestEntity value;
	}

	@Test
	public void simplePropertyInheritanceMapping() {
		final SimpleBasicChildTestEntity child = new SimpleBasicChildTestEntity();
		child.field = "value";
		final PropertyInheritanceTestEntity entity = new PropertyInheritanceTestEntity();
		entity.value = child;
		template.insert(entity);
		final PropertyInheritanceTestEntity document = template
				.find(entity.getId(), PropertyInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(SimpleBasicChildTestEntity.class)));
		assertThat(((SimpleBasicChildTestEntity) document.value).field, is(child.field));
	}

	@Test
	public void complexPropertyInheritanceMapping() {
		final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
		innerChild.field = "value";
		final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
		child.nestedEntity = innerChild;
		final PropertyInheritanceTestEntity entity = new PropertyInheritanceTestEntity();
		entity.value = child;
		template.insert(entity);
		final PropertyInheritanceTestEntity document = template
				.find(entity.getId(), PropertyInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(ComplexBasicChildTestEntity.class)));
		final ComplexBasicChildTestEntity complexDocument = (ComplexBasicChildTestEntity) document.value;
		assertThat(complexDocument.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
		final SimpleBasicChildTestEntity simpleDocument = (SimpleBasicChildTestEntity) complexDocument.nestedEntity;
		assertThat(simpleDocument.field, is(innerChild.field));
	}

	public static class ListInheritanceTestEntity extends BasicTestEntity {
		private List<BasicTestEntity> value;
	}

	@Test
	public void simpleListInheritanceMapping() {
		final List<BasicTestEntity> list = new ArrayList<>();
		final String value = "value";
		for (int i = 0; i < 3; ++i) {
			final SimpleBasicChildTestEntity child = new SimpleBasicChildTestEntity();
			child.field = value;
			list.add(child);
		}
		final ListInheritanceTestEntity entity = new ListInheritanceTestEntity();
		entity.value = list;
		template.insert(entity);
		final ListInheritanceTestEntity document = template.find(entity.getId(), ListInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(List.class)));
		for (final BasicTestEntity elem : document.value) {
			assertThat(elem, is(instanceOf(SimpleBasicChildTestEntity.class)));
			assertThat(((SimpleBasicChildTestEntity) elem).field, is(value));
		}
	}

	@Test
	public void complexListInheritanceMapping() {
		final List<BasicTestEntity> list = new ArrayList<>();
		final String value = "value";
		for (int i = 0; i < 3; ++i) {
			final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
			innerChild.field = value;
			final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
			child.nestedEntity = innerChild;
			list.add(child);
		}
		final ListInheritanceTestEntity entity = new ListInheritanceTestEntity();
		entity.value = list;
		template.insert(entity);
		final ListInheritanceTestEntity document = template.find(entity.getId(), ListInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(List.class)));
		for (final BasicTestEntity elem : document.value) {
			assertThat(elem, is(instanceOf(ComplexBasicChildTestEntity.class)));
			final ComplexBasicChildTestEntity complexElem = (ComplexBasicChildTestEntity) elem;
			assertThat(complexElem.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
			final SimpleBasicChildTestEntity simpleElem = (SimpleBasicChildTestEntity) complexElem.nestedEntity;
			assertThat(simpleElem.field, is(value));
		}
	}

	@SuppressWarnings("rawtypes")
	public static class UntypedListInheritanceTestEntity extends BasicTestEntity {
		private List value;
	}

	@Test
	public void untypedListInheritanceMapping() {
		final List<BasicTestEntity> list = new ArrayList<>();
		final String value = "value";
		for (int i = 0; i < 3; ++i) {
			final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
			innerChild.field = value;
			final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
			child.nestedEntity = innerChild;
			list.add(child);
		}
		final UntypedListInheritanceTestEntity entity = new UntypedListInheritanceTestEntity();
		entity.value = list;
		template.insert(entity);
		final UntypedListInheritanceTestEntity document = template
				.find(entity.getId(), UntypedListInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(List.class)));
		for (final Object elem : document.value) {
			assertThat(elem, is(instanceOf(ComplexBasicChildTestEntity.class)));
			final ComplexBasicChildTestEntity complexElem = (ComplexBasicChildTestEntity) elem;
			assertThat(complexElem.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
			final SimpleBasicChildTestEntity simpleElem = (SimpleBasicChildTestEntity) complexElem.nestedEntity;
			assertThat(simpleElem.field, is(value));
		}
	}

	public static class MapInheritanceTestEntity extends BasicTestEntity {
		private Map<String, BasicTestEntity> value;
	}

	@Test
	public void simpleMapInheritanceMapping() {
		final Map<String, BasicTestEntity> map = new HashMap<>();
		final String value = "value";
		for (int i = 0; i < 3; ++i) {
			final SimpleBasicChildTestEntity child = new SimpleBasicChildTestEntity();
			child.field = value;
			map.put(String.valueOf(i), child);
		}
		final MapInheritanceTestEntity entity = new MapInheritanceTestEntity();
		entity.value = map;
		template.insert(entity);
		final MapInheritanceTestEntity document = template.find(entity.getId(), MapInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(Map.class)));
		for (final Map.Entry<String, BasicTestEntity> entry : document.value.entrySet()) {
			assertThat(entry.getValue(), is(instanceOf(SimpleBasicChildTestEntity.class)));
			assertThat(((SimpleBasicChildTestEntity) entry.getValue()).field, is(value));
		}
	}

	@Test
	public void complexMapInheritanceMapping() {
		final Map<String, BasicTestEntity> map = new HashMap<>();
		final String value = "value";
		for (int i = 0; i < 3; ++i) {
			final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
			innerChild.field = value;
			final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
			child.nestedEntity = innerChild;
			map.put(String.valueOf(i), child);
		}
		final MapInheritanceTestEntity entity = new MapInheritanceTestEntity();
		entity.value = map;
		template.insert(entity);
		final MapInheritanceTestEntity document = template.find(entity.getId(), MapInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(Map.class)));
		for (final Map.Entry<String, BasicTestEntity> entry : document.value.entrySet()) {
			assertThat(entry.getValue(), is(instanceOf(ComplexBasicChildTestEntity.class)));
			final ComplexBasicChildTestEntity complexElem = (ComplexBasicChildTestEntity) entry.getValue();
			assertThat(complexElem.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
			final SimpleBasicChildTestEntity simpleElem = (SimpleBasicChildTestEntity) complexElem.nestedEntity;
			assertThat(simpleElem.field, is(value));
		}
	}

	@SuppressWarnings("rawtypes")
	public static class UntypedMapInheritanceTestEntity extends BasicTestEntity {
		private Map value;
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void untypedMapInheritanceMapping() {
		final Map<String, BasicTestEntity> map = new HashMap<>();
		final String value = "value";
		for (int i = 0; i < 3; ++i) {
			final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
			innerChild.field = value;
			final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
			child.nestedEntity = innerChild;
			map.put(String.valueOf(i), child);
		}
		final UntypedMapInheritanceTestEntity entity = new UntypedMapInheritanceTestEntity();
		entity.value = map;
		template.insert(entity);
		final UntypedMapInheritanceTestEntity document = template
				.find(entity.getId(), UntypedMapInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(Map.class)));
		for (final Object entry : document.value.entrySet()) {
			final Object val = ((Map.Entry) entry).getValue();
			assertThat(val, is(instanceOf(ComplexBasicChildTestEntity.class)));
			final ComplexBasicChildTestEntity complexElem = (ComplexBasicChildTestEntity) val;
			assertThat(complexElem.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
			final SimpleBasicChildTestEntity simpleElem = (SimpleBasicChildTestEntity) complexElem.nestedEntity;
			assertThat(simpleElem.field, is(value));
		}
	}

	public static class ConstructorWithPropertyInheritanceTestEntity extends BasicTestEntity {
		private final BasicTestEntity value;

		public ConstructorWithPropertyInheritanceTestEntity(final BasicTestEntity value) {
			this.value = value;
		}
	}

	@Test
	public void constructorPropertyInheritanceMapping() {
		final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
		innerChild.field = "value";
		final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
		child.nestedEntity = innerChild;
		final ConstructorWithPropertyInheritanceTestEntity entity = new ConstructorWithPropertyInheritanceTestEntity(
				child);
		template.insert(entity);
		final ConstructorWithPropertyInheritanceTestEntity document = template
				.find(entity.getId(), ConstructorWithPropertyInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(ComplexBasicChildTestEntity.class)));
		final ComplexBasicChildTestEntity complexDocument = (ComplexBasicChildTestEntity) document.value;
		assertThat(complexDocument.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
		final SimpleBasicChildTestEntity simpleDocument = (SimpleBasicChildTestEntity) complexDocument.nestedEntity;
		assertThat(simpleDocument.field, is(innerChild.field));
	}

	public static class ListInMapInheritanceTestEntity extends BasicTestEntity {
		private Map<String, List<BasicTestEntity>> value;
	}

	@Test
	public void listInMapInheritanceMapping() {
		final Map<String, List<BasicTestEntity>> map = new HashMap<>();
		final String value = "value";
		for (int i = 0; i < 3; ++i) {
			final List<BasicTestEntity> list = new ArrayList<>();
			map.put(String.valueOf(i), list);
			for (int j = 0; j < 3; ++j) {
				final SimpleBasicChildTestEntity innerChild = new SimpleBasicChildTestEntity();
				innerChild.field = value;
				final ComplexBasicChildTestEntity child = new ComplexBasicChildTestEntity();
				child.nestedEntity = innerChild;
				list.add(child);
			}
		}
		final ListInMapInheritanceTestEntity entity = new ListInMapInheritanceTestEntity();
		entity.value = map;
		template.insert(entity);
		final ListInMapInheritanceTestEntity document = template
				.find(entity.getId(), ListInMapInheritanceTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(instanceOf(Map.class)));
		for (final Map.Entry<String, List<BasicTestEntity>> entry : document.value.entrySet()) {
			assertThat(entry.getValue(), is(instanceOf(List.class)));
			for (final BasicTestEntity elem : entry.getValue()) {
				assertThat(elem, is(instanceOf(ComplexBasicChildTestEntity.class)));
				final ComplexBasicChildTestEntity complexElem = (ComplexBasicChildTestEntity) elem;
				assertThat(complexElem.nestedEntity, is(instanceOf(SimpleBasicChildTestEntity.class)));
				final SimpleBasicChildTestEntity simpleElem = (SimpleBasicChildTestEntity) complexElem.nestedEntity;
				assertThat(simpleElem.field, is(value));
			}
		}
	}

	@Document("overrideDocAn")
	static class SubClassWithOwnDocumentAnnotation extends BasicTestEntity {
	}

	@Test
	public void overrideDocumentAnnotation() {
		final SubClassWithOwnDocumentAnnotation doc = new SubClassWithOwnDocumentAnnotation();
		template.insert(doc);
		assertThat(db.collection("overrideDocAn").exists(), is(true));
		assertThat(db.collection("overrideDocAn").count().getCount(),
			is(1L));
	}

	@Edge("overrideEdgeAn")
	static class SubClassWithOwnEdgeAnnotation extends BasicEdgeTestEntity {
	}

	@Test
	public void overrideEdgeAnnotation() {
		final BasicTestEntity from = new BasicTestEntity();
		final BasicTestEntity to = new BasicTestEntity();
		template.insert(from);
		template.insert(to);

		final SubClassWithOwnEdgeAnnotation edge = new SubClassWithOwnEdgeAnnotation();
		edge.from = from;
		edge.to = to;
		template.insert(edge);
		assertThat(db.collection("overrideEdgeAn").exists(), is(true));
		assertThat(db.collection("overrideEdgeAn").count().getCount(),
			is(1L));
	}

}
