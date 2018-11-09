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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;

import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.core.convert.DBDocumentEntity;
import com.arangodb.velocypack.VPackBuilder;
import com.arangodb.velocypack.VPackSlice;
import com.arangodb.velocypack.ValueType;

/**
 * @author Mark Vollmary
 *
 */
public class CustomMappingTest extends AbstractArangoTest {

	static class TestEntity {

		private final String test;

		public TestEntity(final String test) {
			super();
			this.test = test;
		}

		public String getTest() {
			return test;
		}

	}

	private static final String FIELD = "test";

	@Document
	public static class CustomVPackTestEntity {
		private String value;

		public CustomVPackTestEntity() {
			super();
		}

		public CustomVPackTestEntity(final String value) {
			super();
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(final String value) {
			this.value = value;
		}

	}

	public static class CustomVPackWriteTestConverter implements Converter<CustomVPackTestEntity, VPackSlice> {
		@Override
		public VPackSlice convert(final CustomVPackTestEntity source) {
			return new VPackBuilder().add(ValueType.OBJECT).add(FIELD, source.getValue()).close().slice();
		}
	}

	public static class CustomVPackReadTestConverter implements Converter<VPackSlice, CustomVPackTestEntity> {
		@Override
		public CustomVPackTestEntity convert(final VPackSlice source) {
			return new CustomVPackTestEntity(source.get(FIELD).getAsString());
		}
	}

	@Test
	public void customToVPack() {
		final DocumentEntity meta = template.insert(new CustomVPackTestEntity("abc"));
		final Optional<BaseDocument> doc = template.find(meta.getId(), BaseDocument.class);
		assertThat(doc.isPresent(), is(true));
		assertThat(doc.get().getAttribute(FIELD), is("abc"));
		assertThat(doc.get().getAttribute("value"), is(nullValue()));
	}

	@Test
	public void vpackToCustom() {
		final DocumentEntity meta = template.insert(new TestEntity("abc"));
		final Optional<CustomVPackTestEntity> doc = template.find(meta.getId(), CustomVPackTestEntity.class);
		assertThat(doc.isPresent(), is(true));
		assertThat(doc.get().getValue(), is("abc"));
	}

	@Document
	public static class CustomDBEntityTestEntity {
		private String value;

		public CustomDBEntityTestEntity() {
			super();
		}

		public CustomDBEntityTestEntity(final String value) {
			super();
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(final String value) {
			this.value = value;
		}

	}

	public static class CustomDBEntityWriteTestConverter
			implements Converter<CustomDBEntityTestEntity, DBDocumentEntity> {
		@Override
		public DBDocumentEntity convert(final CustomDBEntityTestEntity source) {
			final DBDocumentEntity entity = new DBDocumentEntity();
			entity.put(FIELD, source.getValue());
			return entity;
		}
	}

	public static class CustomDBEntityReadTestConverter
			implements Converter<DBDocumentEntity, CustomDBEntityTestEntity> {
		@Override
		public CustomDBEntityTestEntity convert(final DBDocumentEntity source) {
			return new CustomDBEntityTestEntity((String) source.get(FIELD));
		}
	}

	@Test
	public void customToDBEntity() {
		final DocumentEntity meta = template.insert(new CustomDBEntityTestEntity("abc"));
		final Optional<BaseDocument> doc = template.find(meta.getId(), BaseDocument.class);
		assertThat(doc.isPresent(), is(true));
		assertThat(doc.get().getAttribute(FIELD), is("abc"));
		assertThat(doc.get().getAttribute("value"), is(nullValue()));
	}

	@Test
	public void vpackToDBEntity() {
		final DocumentEntity meta = template.insert(new TestEntity("abc"));
		final Optional<CustomDBEntityTestEntity> doc = template.find(meta.getId(), CustomDBEntityTestEntity.class);
		assertThat(doc.isPresent(), is(true));
		assertThat(doc.get().getValue(), is("abc"));
	}

}
