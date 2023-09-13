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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Optional;

import com.arangodb.ArangoCollection;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.core.convert.DBDocumentEntity;

/**
 * @author Mark Vollmary
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
    public static class CustomJsonNodeTestEntity {
        private String value;

        public CustomJsonNodeTestEntity() {
            super();
        }

        public CustomJsonNodeTestEntity(final String value) {
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

    public static class CustomJsonNodeWriteTestConverter implements Converter<CustomJsonNodeTestEntity, JsonNode> {
        @Override
        public JsonNode convert(final CustomJsonNodeTestEntity source) {
            return JsonNodeFactory.instance.objectNode().put(FIELD, source.getValue());
        }
    }

    public static class CustomJsonNodeReadTestConverter implements Converter<JsonNode, CustomJsonNodeTestEntity> {
        @Override
        public CustomJsonNodeTestEntity convert(final JsonNode source) {
            return new CustomJsonNodeTestEntity(source.get(FIELD).textValue());
        }
    }

    @Test
    public void customToJsonNode() {
        final DocumentEntity meta = template.insert(new CustomJsonNodeTestEntity("abc"));
        final Optional<BaseDocument> doc = template.find(meta.getId(), BaseDocument.class);
        assertThat(doc.isPresent(), is(true));
        assertThat(doc.get().getAttribute(FIELD), is("abc"));
        assertThat(doc.get().getAttribute("value"), is(nullValue()));
    }

    @Test
    public void jsonNodeToCustom() {
        final DocumentEntity meta = template.insert(new TestEntity("abc"));
        final Optional<CustomJsonNodeTestEntity> doc = template.find(meta.getId(), CustomJsonNodeTestEntity.class);
        assertThat(doc.isPresent(), is(true));
        assertThat(doc.get().getValue(), is("abc"));
    }

    @Test
    public void customToJsonNodeFromDriver() {
        ArangoCollection col = template.driver().db(ArangoTestConfiguration.DB).collection("customJsonNodeTestEntity");
        final DocumentEntity meta = col.insertDocument(new CustomJsonNodeTestEntity("abc"));
        final BaseDocument doc = col.getDocument(meta.getKey(), BaseDocument.class);
        assertThat(doc.getAttribute(FIELD), is("abc"));
        assertThat(doc.getAttribute("value"), is(nullValue()));
    }

    @Test
    public void jsonNodeToCustomFromDriver() {
        ArangoCollection col = template.driver().db(ArangoTestConfiguration.DB).collection("testEntity");
        final DocumentEntity meta = col.insertDocument(new TestEntity("abc"));
        final CustomJsonNodeTestEntity doc = col.getDocument(meta.getKey(), CustomJsonNodeTestEntity.class);
        assertThat(doc.getValue(), is("abc"));
    }

    @Document()
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
    public void jsonNodeToDBEntity() {
        final DocumentEntity meta = template.insert(new TestEntity("abc"));
        final Optional<CustomDBEntityTestEntity> doc = template.find(meta.getId(), CustomDBEntityTestEntity.class);
        assertThat(doc.isPresent(), is(true));
        assertThat(doc.get().getValue(), is("abc"));
    }

    @Test
    public void customToDBEntityFromDriver() {
        ArangoCollection col = template.driver().db(ArangoTestConfiguration.DB).collection("customDBEntityTestEntity");
        final DocumentEntity meta = col.insertDocument(new CustomDBEntityTestEntity("abc"));
        final BaseDocument doc = col.getDocument(meta.getKey(), BaseDocument.class);
        assertThat(doc.getAttribute(FIELD), is("abc"));
        assertThat(doc.getAttribute("value"), is(nullValue()));
    }

    @Test
    public void jsonNodeToDBEntityFromDriver() {
        ArangoCollection col = template.driver().db(ArangoTestConfiguration.DB).collection("testEntity");
        final DocumentEntity meta = col.insertDocument(new TestEntity("abc"));
        final CustomDBEntityTestEntity doc = col.getDocument(meta.getKey(), CustomDBEntityTestEntity.class);
        assertThat(doc.getValue(), is("abc"));
    }

}
