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

package com.arangodb.springframework.core.mapping;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.entity.DocumentEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.annotation.Field;
import com.arangodb.springframework.core.mapping.testdata.BasicTestEntity;
import com.arangodb.springframework.testdata.Actor;
import com.arangodb.springframework.testdata.Movie;
import com.arangodb.springframework.testdata.Role;
import com.arangodb.util.MapBuilder;
import com.arangodb.velocypack.VPackSlice;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class ArangoMappingTest extends AbstractArangoTest {

	@Test
	public void idKeyRev() {
		final DocumentEntity ref = template.insert(new BasicTestEntity());
		final BasicTestEntity entity = template.find(ref.getId(), BasicTestEntity.class).get();
		assertThat(entity, is(notNullValue()));
		assertThat(entity.getId(), is(ref.getId()));
		assertThat(entity.getKey(), is(ref.getKey()));
		assertThat(entity.getRev(), is(ref.getRev()));
	}

	public static class OnlyIdTestEntity {
		@Id
		private String id;
	}

	@Test
	public void supplementKey() {
		final OnlyIdTestEntity value = new OnlyIdTestEntity();
		template.insert(value);
		final List<BasicTestEntity> result = template.query("RETURN @doc", new MapBuilder().put("doc", value).get(),
			new AqlQueryOptions(), BasicTestEntity.class).asListRemaining();
		assertThat(result.size(), is(1));
		assertThat(result.get(0).getId(), is(value.id));
		assertThat(result.get(0).getKey(), is(value.id.split("/")[1]));
		assertThat(result.get(0).getRev(), is(nullValue()));
	}

	public static class FieldNameTestEntity extends BasicTestEntity {
		@Field("alt-test")
		private String test;
	}

	@Test
	public void fieldNameAnnotation() {
		final FieldNameTestEntity entity = new FieldNameTestEntity();
		entity.test = "1234";
		final DocumentEntity res = template.insert(entity);
		final VPackSlice slice = template.driver().db(ArangoTestConfiguration.DB).getDocument(res.getId(),
			VPackSlice.class);
		assertThat(slice, is(notNullValue()));
		assertThat(slice.get("alt-test").isString(), is(true));
		assertThat(slice.get("alt-test").getAsString(), is(entity.test));
	}

	public static class SingleNestedDocumentTestEntity extends BasicTestEntity {
		private NestedDocumentTestEntity entity;
	}

	public static class NestedDocumentTestEntity {
		private String test;

		public NestedDocumentTestEntity() {
			super();
		}

		public NestedDocumentTestEntity(final String test) {
			super();
			this.test = test;
		}
	}

	@Test
	public void singleNestedDocument() {
		final SingleNestedDocumentTestEntity entity = new SingleNestedDocumentTestEntity();
		entity.entity = new NestedDocumentTestEntity("test");
		template.insert(entity);
		final SingleNestedDocumentTestEntity document = template.find(entity.id, SingleNestedDocumentTestEntity.class)
				.get();
		assertThat(document, is(notNullValue()));
		assertThat(document.entity, is(notNullValue()));
		assertThat(document.entity.test, is("test"));
	}

	public static class MultipleNestedDocumentTestEntity extends BasicTestEntity {
		private Collection<NestedDocumentTestEntity> entities;
	}

	@Test
	public void multipleNestedDocuments() {
		final MultipleNestedDocumentTestEntity entity = new MultipleNestedDocumentTestEntity();
		entity.entities = new ArrayList<>(Arrays.asList(new NestedDocumentTestEntity("0"),
			new NestedDocumentTestEntity("1"), new NestedDocumentTestEntity("2")));
		template.insert(entity);
		final MultipleNestedDocumentTestEntity document = template
				.find(entity.id, MultipleNestedDocumentTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		assertThat(document.entities.size(), is(3));
		assertThat(document.entities.stream().map(e -> e.test).collect(Collectors.toList()), hasItems("0", "1", "2"));
	}

	public static class MultipleNestedCollectionsTestEntity extends BasicTestEntity {
		private Collection<Collection<NestedDocumentTestEntity>> entities;
	}

	@Test
	public void multipleNestedCollections() {
		final MultipleNestedCollectionsTestEntity entity = new MultipleNestedCollectionsTestEntity();
		entity.entities = new ArrayList<>(Arrays.asList(
			new ArrayList<>(Arrays.asList(new NestedDocumentTestEntity("00"), new NestedDocumentTestEntity("01"),
				new NestedDocumentTestEntity("02"))),
			new ArrayList<>(Arrays.asList(new NestedDocumentTestEntity("10"), new NestedDocumentTestEntity("11"),
				new NestedDocumentTestEntity("12")))));
		template.insert(entity);
		final MultipleNestedCollectionsTestEntity document = template
				.find(entity.id, MultipleNestedCollectionsTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		assertThat(document.entities.size(), is(2));
		final List<List<String>> collect = document.entities.stream()
				.map(c -> c.stream().map(cc -> cc.test).collect(Collectors.toList())).collect(Collectors.toList());
		for (int i = 0; i < collect.size(); i++) {
			for (int j = 0; j < collect.get(i).size(); j++) {
				assertThat(collect.get(i).get(j), is(i + "" + j));
			}
		}
	}

	public static class SingleNestedMapTestEntity extends BasicTestEntity {
		private Map<String, NestedDocumentTestEntity> entities;
	}

	@Test
	public void singleNestedMap() {
		final SingleNestedMapTestEntity entity = new SingleNestedMapTestEntity();
		entity.entities = new HashMap<>();
		entity.entities.put("0", new NestedDocumentTestEntity("0"));
		entity.entities.put("1", new NestedDocumentTestEntity("1"));
		entity.entities.put("2", new NestedDocumentTestEntity("2"));
		template.insert(entity);
		final SingleNestedMapTestEntity document = template.find(entity.id, SingleNestedMapTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		final Map<String, String> collect = document.entities.entrySet().stream()
				.map(e -> new String[] { e.getKey(), e.getValue().test })
				.collect(Collectors.toMap(k -> k[0], v -> v[1]));
		for (int i = 0; i <= 2; i++) {
			assertThat(collect.get(String.valueOf(i)), is(String.valueOf(i)));
		}
	}

	public static class MultipleNestedMapTestEntity extends BasicTestEntity {
		private Map<String, Map<String, NestedDocumentTestEntity>> entities;
	}

	@Test
	public void multipleNestedMaps() {
		final MultipleNestedMapTestEntity entity = new MultipleNestedMapTestEntity();
		entity.entities = new HashMap<>();
		final Map<String, NestedDocumentTestEntity> m0 = new HashMap<>();
		m0.put("0", new NestedDocumentTestEntity("00"));
		m0.put("1", new NestedDocumentTestEntity("01"));
		m0.put("2", new NestedDocumentTestEntity("02"));
		entity.entities.put("0", m0);
		final Map<String, NestedDocumentTestEntity> m1 = new HashMap<>();
		m1.put("0", new NestedDocumentTestEntity("10"));
		m1.put("1", new NestedDocumentTestEntity("11"));
		m1.put("2", new NestedDocumentTestEntity("12"));
		entity.entities.put("1", m1);
		template.insert(entity);
		final MultipleNestedMapTestEntity document = template.find(entity.id, MultipleNestedMapTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.entities, is(notNullValue()));
		for (int i = 0; i <= 1; i++) {
			for (int j = 0; j <= 2; j++) {
				assertThat(document.entities.get(String.valueOf(i)).get(String.valueOf(j)).test, is(i + "" + j));
			}
		}
	}

	public static class ConstructorWithParamTestEntity extends BasicTestEntity {
		private final String value;

		public ConstructorWithParamTestEntity(final String value) {
			super();
			this.value = value;
		}
	}

	@Test
	public void constructorWithParam() {
		final ConstructorWithParamTestEntity entity = new ConstructorWithParamTestEntity("test");
		template.insert(entity);
		final ConstructorWithParamTestEntity document = template
				.find(entity.getId(), ConstructorWithParamTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value, is(entity.value));
	}

	public static class ConstructorWithMultipleParamsTestEntity extends BasicTestEntity {
		private final String value1;
		private final boolean value2;
		private final double value3;
		private final long value4;
		private final int value5;
		private final String[] value6;

		public ConstructorWithMultipleParamsTestEntity(final String value1, final boolean value2, final double value3,
			final long value4, final int value5, final String[] value6) {
			super();
			this.value1 = value1;
			this.value2 = value2;
			this.value3 = value3;
			this.value4 = value4;
			this.value5 = value5;
			this.value6 = value6;
		}

	}

	@Test
	public void constructorWithMultipleParams() {
		final ConstructorWithMultipleParamsTestEntity entity = new ConstructorWithMultipleParamsTestEntity("test", true,
				3.5, 13L, 69, new String[] { "a", "b" });
		template.insert(entity);
		final ConstructorWithMultipleParamsTestEntity document = template
				.find(entity.getId(), ConstructorWithMultipleParamsTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value1, is(entity.value1));
		assertThat(document.value2, is(entity.value2));
		assertThat(document.value3, is(entity.value3));
		assertThat(document.value4, is(entity.value4));
		assertThat(document.value5, is(entity.value5));
		assertThat(document.value6, is(entity.value6));
	}

	public static class JodaTestEntity extends BasicTestEntity {
		private org.joda.time.DateTime value1;
		private org.joda.time.Instant value2;
		private org.joda.time.LocalDate value3;
		private org.joda.time.LocalDateTime value4;
	}

	@Test
	public void jodaMapping() {
		final JodaTestEntity entity = new JodaTestEntity();
		entity.value1 = org.joda.time.DateTime.now(DateTimeZone.forOffsetHours(1));
		entity.value2 = org.joda.time.Instant.now();
		entity.value3 = org.joda.time.LocalDate.now();
		entity.value4 = org.joda.time.LocalDateTime.now();
		template.insert(entity);
		final JodaTestEntity document = template.find(entity.getId(), JodaTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value1, is(entity.value1));
		assertThat(document.value2, is(entity.value2));
		assertThat(document.value3, is(entity.value3));
		assertThat(document.value4, is(entity.value4));
	}

	public static class Java8TimeTestEntity extends BasicTestEntity {
		private java.time.Instant value1;
		private java.time.LocalDate value2;
		private java.time.LocalDateTime value3;
	}

	@Test
	public void timeMapping() {
		final Java8TimeTestEntity entity = new Java8TimeTestEntity();
		entity.value1 = java.time.Instant.now();
		entity.value2 = java.time.LocalDate.now();
		entity.value3 = java.time.LocalDateTime.now();
		template.insert(entity);
		final Java8TimeTestEntity document = template.find(entity.getId(), Java8TimeTestEntity.class).get();
		assertThat(document, is(notNullValue()));
		assertThat(document.value1, is(entity.value1));
		assertThat(document.value2, is(entity.value2));
		assertThat(document.value3, is(entity.value3));
	}

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

	public class SimpleTypesTestEntity extends BasicTestEntity {
		private String stringValue;
		private Boolean boolValue;
		private int intValue;
		private Long longValue;
		private Short shortValue;
		private Float floatValue;
		private Double doubleValue;
		private Character charValue;
		private Byte byteValue;
		private BigInteger bigIntValue;
		private BigDecimal bigDecValue;
		private UUID uuidValue;
		private Date dateValue;
		private java.sql.Date sqlDateValue;
		private Timestamp timestampValue;
		private byte[] byteArray;
	}

	@Test
	public void simpleTypesMapping() {
		final SimpleTypesTestEntity entity = new SimpleTypesTestEntity();
		entity.stringValue = "hello world";
		entity.boolValue = true;
		entity.intValue = 123456;
		entity.longValue = 1234567890123456789l;
		entity.shortValue = 1234;
		entity.floatValue = 1.234567890f;
		entity.doubleValue = 1.2345678901234567890;
		entity.charValue = 'a';
		entity.byteValue = 'z';
		entity.bigIntValue = new BigInteger("123456789");
		entity.bigDecValue = new BigDecimal("1.23456789");
		entity.uuidValue = UUID.randomUUID();
		entity.dateValue = new Date();
		entity.sqlDateValue = new java.sql.Date(new Date().getTime());
		entity.timestampValue = new Timestamp(new Date().getTime());
		entity.byteArray = new byte[] { 'a', 'b', 'c', 'x', 'y', 'z' };
		template.insert(entity);
		final SimpleTypesTestEntity document = template.find(entity.getId(), SimpleTypesTestEntity.class).get();
		assertThat(entity.stringValue, is(document.stringValue));
		assertThat(entity.boolValue, is(document.boolValue));
		assertThat(entity.intValue, is(document.intValue));
		assertThat(entity.longValue, is(document.longValue));
		assertThat(entity.shortValue, is(document.shortValue));
		assertThat(entity.floatValue, is(document.floatValue));
		assertThat(entity.doubleValue, is(document.doubleValue));
		assertThat(entity.charValue, is(document.charValue));
		assertThat(entity.byteValue, is(document.byteValue));
		assertThat(entity.bigIntValue, is(document.bigIntValue));
		assertThat(entity.bigDecValue, is(document.bigDecValue));
		assertThat(entity.uuidValue, is(document.uuidValue));
		assertThat(entity.dateValue, is(document.dateValue));
		assertThat(entity.sqlDateValue, is(document.sqlDateValue));
		assertThat(entity.timestampValue, is(document.timestampValue));
		assertThat(entity.byteArray, is(document.byteArray));
	}

	public enum TestEnum {
		A, B;
	}

	public class EnumTestEntity extends BasicTestEntity {
		private TestEnum value;
	}

	@Test
	public void enumMapping() {
		final EnumTestEntity entity = new EnumTestEntity();
		entity.value = TestEnum.A;
		template.insert(entity);
		final EnumTestEntity document = template.find(entity.getId(), EnumTestEntity.class).get();
		assertThat(entity.value, is(document.value));
	}

	@Test
	public void cyclicRelationsTest() {
		final Actor actor = new Actor();
		actor.setName("George Clooney");
		template.insert(actor);

		final Movie movie1 = new Movie();
		movie1.setName("Ocean's Eleven");
		template.insert(movie1);

		final Movie movie2 = new Movie();
		movie2.setName("Ocean's Twelve");
		template.insert(movie2);

		final Movie movie3 = new Movie();
		movie3.setName("Ocean's Thirteen");
		template.insert(movie3);

		final Role role1 = new Role();
		role1.setActor(actor);
		role1.setMovie(movie1);
		template.insert(role1);

		final Role role2 = new Role();
		role2.setActor(actor);
		role2.setMovie(movie2);
		template.insert(role2);

		final Role role3 = new Role();
		role3.setActor(actor);
		role3.setMovie(movie3);
		template.insert(role3);

		final Actor retrieved = template.find(actor.getId(), Actor.class).get();

		assertThat(retrieved, is(notNullValue()));

		assertThat(retrieved.getId(), is(actor.getId()));
		assertThat(retrieved.getName(), is(actor.getName()));

		assertThat(retrieved.getRoles(), is(notNullValue()));
		assertThat(retrieved.getMovies(), is(notNullValue()));

		for (final Role role : retrieved.getRoles()) {
			assertThat(role.getActor(), is(notNullValue()));
			assertThat(role.getActor().getId(), is(actor.getId()));
			assertThat(role.getActor().getName(), is(actor.getName()));

			assertThat(role.getMovie(), is(notNullValue()));
			assertThat(role.getMovie().getId(), isOneOf(movie1.getId(), movie2.getId(), movie3.getId()));
			assertThat(role.getMovie().getName(), isOneOf(movie1.getName(), movie2.getName(), movie3.getName()));
		}

		for (final Movie movie : retrieved.getMovies()) {
			assertThat(movie, is(notNullValue()));
			assertThat(movie.getId(), isOneOf(movie1.getId(), movie2.getId(), movie3.getId()));
			assertThat(movie.getName(), isOneOf(movie1.getName(), movie2.getName(), movie3.getName()));

			assertThat(movie.getActors(), is(notNullValue()));
			for (final Actor a : movie.getActors()) {
				assertThat(a, is(notNullValue()));
				assertThat(a.getId(), isOneOf(actor.getId()));
				assertThat(a.getName(), isOneOf(actor.getName()));
			}
		}

	}
}
