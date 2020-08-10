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

import com.arangodb.entity.DocumentEntity;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.AuditorProvider;
import com.arangodb.springframework.annotation.ArangoId;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Field;
import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.core.ArangoOperations.UpsertStrategy;
import com.arangodb.springframework.core.mapping.testdata.BasicTestEntity;
import com.arangodb.springframework.testdata.Actor;
import com.arangodb.springframework.testdata.Movie;
import com.arangodb.springframework.testdata.Person;
import com.arangodb.springframework.testdata.Role;
import com.arangodb.util.MapBuilder;
import com.arangodb.velocypack.VPackSlice;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 *
 */
public class GeneralMappingTest extends AbstractArangoTest {

	@Test
	public void idKeyRev() {
		final DocumentEntity ref = template.insert(new BasicTestEntity());
		final BasicTestEntity entity = template.find(ref.getId(), BasicTestEntity.class).get();
		assertThat(entity, is(notNullValue()));
		assertThat(entity.getId(), is(ref.getKey()));
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

	@Test
	public void twoTypesInSameCollection() {
		final TwoTypesInSameCollectionA a = new TwoTypesInSameCollectionA();
		a.value = "testA";
		a.a = "testA";
		final TwoTypesInSameCollectionB b = new TwoTypesInSameCollectionB();
		b.value = "testB";
		b.b = "testB";

		template.insert(a);
		template.insert(b);
		final Optional<TwoTypesInSameCollectionA> findA = template.find(a.getId(), TwoTypesInSameCollectionA.class);
		assertThat(findA.isPresent(), is(true));
		assertThat(findA.get().value, is("testA"));
		assertThat(findA.get().a, is("testA"));
		final Optional<TwoTypesInSameCollectionB> findB = template.find(b.getId(), TwoTypesInSameCollectionB.class);
		assertThat(findB.isPresent(), is(true));
		assertThat(findB.get().value, is("testB"));
		assertThat(findB.get().b, is("testB"));
	}

	static class ArangoIdOnlyTestEntity {
		@ArangoId
		private String id;
	}

	@SuppressWarnings("deprecation")
	@Test
	public void arangoIdOnly() {
		final ArangoIdOnlyTestEntity entity = new ArangoIdOnlyTestEntity();
		template.upsert(entity, UpsertStrategy.UPDATE);
		assertThat(entity.id, is(notNullValue()));
		final Optional<ArangoIdOnlyTestEntity> find = template.find(entity.id, ArangoIdOnlyTestEntity.class);
		assertThat(find.isPresent(), is(true));
		assertThat(find.get().id, is(entity.id));
	}

	static class ArangoIdAndIdTestEntity {
		@ArangoId
		private String arangoId;
		@Id
		private String id;
	}

	@Test
	public void arangoIdAndId() {
		final ArangoIdAndIdTestEntity entity = new ArangoIdAndIdTestEntity();
		entity.arangoId = "arangoIdAndIdTestEntity/test";
		template.insert(entity);
		assertThat(entity.arangoId, is("arangoIdAndIdTestEntity/test"));
		assertThat(entity.id, is("test"));
		assertThat(template.find(entity.id, ArangoIdAndIdTestEntity.class).isPresent(), is(true));
		assertThat(template.find(entity.arangoId, ArangoIdAndIdTestEntity.class).isPresent(), is(true));
	}

	@Document
	static class AuditingTestEntity {
		@Id
		private String id;
		@CreatedDate
		private Instant created;
		@CreatedBy
		private Person createdBy;
		@LastModifiedDate
		private Instant modified;
		@Ref
		@LastModifiedBy
		private Person modifiedBy;
	}

	@Autowired
	AuditorProvider auditorProvider;

	@Test
	public void auditingTest() throws InterruptedException {
		final String createID = "create";
		{
			final Person person = new Person();
			person.setId(createID);
			template.insert(person);
			auditorProvider.setPerson(person);
		}

		final Instant beforeInsert = Instant.now();
		final AuditingTestEntity value = new AuditingTestEntity();
		template.insert(value);
		final Instant afterInsert = Instant.now();
		{
			final AuditingTestEntity find = template.find(value.id, AuditingTestEntity.class).get();
			assertThat(find.created, is(notNullValue()));
			assertThat(find.created.isAfter(beforeInsert), is(true));
			assertThat(find.created.isBefore(afterInsert), is(true));

			assertThat(find.createdBy, is(notNullValue()));
			assertThat(find.createdBy.getId(), is(createID));

			assertThat(find.modified, is(notNullValue()));
			assertThat(find.modified.isAfter(beforeInsert), is(true));
			assertThat(find.modified.isBefore(afterInsert), is(true));

			assertThat(find.modifiedBy, is(notNullValue()));
			assertThat(find.modifiedBy.getId(), is(createID));
		}

		final VPackSlice doc = template.driver().db(ArangoTestConfiguration.DB).collection("auditingTestEntity")
				.getDocument(value.id, VPackSlice.class);
		assertThat(doc, is(notNullValue()));
		assertThat(doc.get("createdBy").isObject(), is(true));
		assertThat(doc.get("modifiedBy").isString(), is(true));

		final String modifiedID = "modified";
		{
			final Person person = new Person();
			person.setId(modifiedID);
			template.insert(person);
			auditorProvider.setPerson(person);
		}

		final Instant beforeReplace = Instant.now();
		Thread.sleep(1);
		template.replace(value.id, value);
		Thread.sleep(1);
		final Instant afterReplace = Instant.now();
		{
			final AuditingTestEntity find = template.find(value.id, AuditingTestEntity.class).get();
			assertThat(find.created, is(notNullValue()));
			assertThat(find.created.isAfter(beforeInsert), is(true));
			assertThat(find.created.isBefore(afterInsert), is(true));

			assertThat(find.createdBy, is(notNullValue()));
			assertThat(find.createdBy.getId(), is(createID));

			assertThat(find.modified, is(notNullValue()));
			assertThat(find.modified.isAfter(beforeReplace), is(true));
			assertThat(find.modified.isBefore(afterReplace), is(true));

			assertThat(find.modifiedBy, is(notNullValue()));
			assertThat(find.modifiedBy.getId(), is(modifiedID));
		}
	}

	@Document("primitive_test_col")
	static class NoPrimitiveTestEntity {
		@Id
		private String id;
	}

	@Document("primitive_test_col")
	static class PrimitiveTestEntity {
		@Id
		private String id;
		@SuppressWarnings("unused")
		private boolean value;
	}

	@Test
	public void readNonExistingPrimitive() {
		final NoPrimitiveTestEntity entity = new NoPrimitiveTestEntity();
		template.insert(entity);
		final Optional<PrimitiveTestEntity> find = template.find(entity.id, PrimitiveTestEntity.class);
		assertThat(find.isPresent(), is(true));
		assertThat(find.get().id, is(entity.id));
	}

	@Document
	static class ObjectFieldTestEntity {
		@Id
		private String id;
		private Object value;
	}

	@Test
	public void readObjectFieldFromString() {
		final ObjectFieldTestEntity entity = new ObjectFieldTestEntity();
		entity.value = "test";
		template.insert(entity);
		final Optional<ObjectFieldTestEntity> find = template.find(entity.id, ObjectFieldTestEntity.class);
		assertThat(find.isPresent(), is(true));
		assertThat(find.get().value, is("test"));
	}

	@Test
	public void readObjectFieldFromMap() {
		final ObjectFieldTestEntity entity = new ObjectFieldTestEntity();
		final HashMap<Object, Object> map = new HashMap<>();
		map.put("key", "value");
		entity.value = map;
		template.insert(entity);
		final Optional<ObjectFieldTestEntity> find = template.find(entity.id, ObjectFieldTestEntity.class);
		assertThat(find.isPresent(), is(true));
		assertThat(find.get().value, is(map));
	}

	@Test
	public void readObjectFieldFromCollectionLike() {
		final ObjectFieldTestEntity entity = new ObjectFieldTestEntity();
		final Collection<String> collection = Arrays.asList("test", "test");
		entity.value = collection;
		template.insert(entity);
		final Optional<ObjectFieldTestEntity> find = template.find(entity.id, ObjectFieldTestEntity.class);
		assertThat(find.isPresent(), is(true));
		assertThat(find.get().value, is(collection));
	}

	@Test
	public void readObjectFieldFromPOJO() {
		final ObjectFieldTestEntity entity = new ObjectFieldTestEntity();
		final SimpleTypesTestEntity value = new SimpleTypesTestEntity();
		value.stringValue = "test";
		entity.value = value;
		template.insert(entity);
		final Optional<ObjectFieldTestEntity> find = template.find(entity.id, ObjectFieldTestEntity.class);
		assertThat(find.isPresent(), is(true));
		assertThat(find.get().value instanceof SimpleTypesTestEntity, is(true));
		assertThat(SimpleTypesTestEntity.class.cast(find.get().value).stringValue, is("test"));
	}

	@Document
	static class MapInMapTestEntity {
		@Id
		private String id;
		private Map<String, Object> value;
	}

	@Test
	public void readMapInMap() {
		final MapInMapTestEntity entity = new MapInMapTestEntity();
		final Map<String, Object> map = new HashMap<>();
		map.put("map", new HashMap<>());
		entity.value = map;
		template.insert(entity);
		final Optional<MapInMapTestEntity> find = template.find(entity.id, MapInMapTestEntity.class);
		assertThat(find.isPresent(), is(true));
		assertThat(find.get().value, is(map));
	}

	@Test
	public void readMapInMapWithNull() {
		final MapInMapTestEntity entity = new MapInMapTestEntity();
		final Map<String, Object> map = new HashMap<>();
		Map<String, Object> nested = new HashMap<>();
		nested.put("key1", "test");
		nested.put("key2", null);
		map.put("nested", nested);
		entity.value = map;
		template.insert(entity);
		final Optional<MapInMapTestEntity> find = template.find(entity.id, MapInMapTestEntity.class);
		assertThat(find.isPresent(), is(true));
		Map<String, Object> nestedResult = (Map<String, Object>) find.get().value.get("nested");
		assertThat(nestedResult.size(), is(1));
		assertThat(nestedResult.get("key1"), is("test"));
	}

	@Test
	public void readMapInMapWithArray() {
		final MapInMapTestEntity entity = new MapInMapTestEntity();
		final Map<String, Object> map = new HashMap<>();
		Map<String, Object> nested = new HashMap<>();
		String[] nestedArray = {"test", null};
		nested.put("key1", nestedArray);
		map.put("nested", nested);
		entity.value = map;
		template.insert(entity);
		final Optional<MapInMapTestEntity> find = template.find(entity.id, MapInMapTestEntity.class);
		assertThat(find.isPresent(), is(true));
		Map<String, Object> nestedResult = (Map<String, Object>) find.get().value.get("nested");
		assertThat(nestedResult.size(), is(1));
		assertThat(((List<String>) nestedResult.get("key1")).size(), is(2));
		assertEquals(Arrays.asList(nestedArray), nestedResult.get("key1"));
	}

	@Test
	public void readMapInMapWithCollection() {
		final MapInMapTestEntity entity = new MapInMapTestEntity();
		final Map<String, Object> map = new HashMap<>();
		Map<String, Object> nested = new HashMap<>();
		Collection<String> nestedCollection = Arrays.asList("test", null);
		nested.put("key1", nestedCollection);
		map.put("nested", nested);
		entity.value = map;
		template.insert(entity);
		final Optional<MapInMapTestEntity> find = template.find(entity.id, MapInMapTestEntity.class);
		assertThat(find.isPresent(), is(true));
		Map<String, Object> nestedResult = (Map<String, Object>) find.get().value.get("nested");
		assertThat(nestedResult.size(), is(1));
		assertThat(((List<String>) nestedResult.get("key1")).size(), is(2));
		assertEquals(nestedCollection, nestedResult.get("key1"));
	}
}
