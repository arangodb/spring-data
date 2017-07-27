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

package com.arangodb.springframework.core.template.java;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.entity.IndexType;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.annotation.FulltextIndex;
import com.arangodb.springframework.annotation.FulltextIndexed;
import com.arangodb.springframework.annotation.FulltextIndexes;
import com.arangodb.springframework.annotation.GeoIndex;
import com.arangodb.springframework.annotation.GeoIndexed;
import com.arangodb.springframework.annotation.GeoIndexes;
import com.arangodb.springframework.annotation.HashIndex;
import com.arangodb.springframework.annotation.HashIndexed;
import com.arangodb.springframework.annotation.HashIndexes;
import com.arangodb.springframework.annotation.PersistentIndex;
import com.arangodb.springframework.annotation.PersistentIndexed;
import com.arangodb.springframework.annotation.PersistentIndexes;
import com.arangodb.springframework.annotation.SkiplistIndex;
import com.arangodb.springframework.annotation.SkiplistIndexed;
import com.arangodb.springframework.annotation.SkiplistIndexes;

/**
 * @author Mark Vollmary
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class ArangoIndexTest extends AbstractArangoTest {

	public static class HashIndexedSingleFieldTestEntity {
		@HashIndexed
		private String a;
	}

	@Test
	public void singleFieldHashIndexed() {
		assertThat(template.collection(HashIndexedSingleFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(HashIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash));
		assertThat(template.collection(HashIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.hash).findFirst().get().getFields(),
			hasItems("a"));
	}

	public static class HashIndexedMultipleSingleFieldTestEntity {
		@HashIndexed
		private String a;
		@HashIndexed
		private String b;
	}

	@Test
	public void multipleSingleFieldHashIndexed() {
		assertThat(template.collection(HashIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(template.collection(HashIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash));
	}

	@HashIndex(fields = { "a" })
	public static class HashIndexWithSingleFieldTestEntity {
	}

	@Test
	public void singleFieldHashIndex() {
		assertThat(template.collection(HashIndexWithSingleFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(HashIndexWithSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash));
		assertThat(template.collection(HashIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.hash).findFirst().get().getFields(),
			hasItems("a"));
	}

	@HashIndex(fields = { "a" })
	@HashIndex(fields = { "b" })
	public static class HashIndexWithMultipleSingleFieldTestEntity {
	}

	@Test
	public void multipleSingleFieldHashIndex() {
		assertThat(template.collection(HashIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(template.collection(HashIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash));
	}

	@HashIndex(fields = { "a", "b" })
	public static class HashIndexWithMultiFieldTestEntity {
	}

	@Test
	public void multiFieldHashIndex() {
		assertThat(template.collection(HashIndexWithMultiFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(HashIndexWithMultiFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash));
		assertThat(template.collection(HashIndexWithMultiFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.hash).findFirst().get().getFields(),
			hasItems("a", "b"));
	}

	@HashIndexes({ @HashIndex(fields = { "a" }), @HashIndex(fields = { "b" }) })
	public static class HashIndexWithMultipleIndexesTestEntity {
	}

	@Test
	public void multipleIndexesHashIndex() {
		assertThat(template.collection(HashIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(template.collection(HashIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash));
	}

	public static class SkiplistIndexedSingleFieldTestEntity {
		@SkiplistIndexed
		private String a;
	}

	@Test
	public void singleFieldSkiplistIndexed() {
		assertThat(template.collection(SkiplistIndexedSingleFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(SkiplistIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.skiplist));
		assertThat(template.collection(SkiplistIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.skiplist).findFirst().get().getFields(),
			hasItems("a"));
	}

	public static class SkiplistIndexedMultipleSingleFieldTestEntity {
		@SkiplistIndexed
		private String a;
		@SkiplistIndexed
		private String b;
	}

	@Test
	public void multipleSingleFieldSkiplistIndexed() {
		assertThat(template.collection(SkiplistIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(
			template.collection(SkiplistIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
					.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.skiplist));
	}

	@SkiplistIndex(fields = { "a" })
	public static class SkiplistIndexWithSingleFieldTestEntity {
	}

	@Test
	public void singleFieldSkiplistIndex() {
		assertThat(template.collection(SkiplistIndexWithSingleFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(
			template.collection(SkiplistIndexWithSingleFieldTestEntity.class).getIndexes().stream()
					.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.skiplist));
		assertThat(template.collection(SkiplistIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.skiplist).findFirst().get().getFields(),
			hasItems("a"));
	}

	@SkiplistIndex(fields = { "a" })
	@SkiplistIndex(fields = { "b" })
	public static class SkiplistIndexWithMultipleSingleFieldTestEntity {
	}

	@Test
	public void multipleSingleFieldSkiplistIndex() {
		assertThat(template.collection(SkiplistIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(
			template.collection(SkiplistIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
					.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.skiplist));
	}

	@SkiplistIndex(fields = { "a", "b" })
	public static class SkiplistIndexWithMultiFieldTestEntity {
	}

	@Test
	public void multiFieldSkiplistIndex() {
		assertThat(template.collection(SkiplistIndexWithMultiFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(SkiplistIndexWithMultiFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.skiplist));
		assertThat(template.collection(SkiplistIndexWithMultiFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.skiplist).findFirst().get().getFields(),
			hasItems("a", "b"));
	}

	@SkiplistIndexes({ @SkiplistIndex(fields = { "a" }), @SkiplistIndex(fields = { "b" }) })
	public static class SkiplistIndexWithMultipleIndexesTestEntity {
	}

	@Test
	public void multipleIndexesSkiplistIndex() {
		assertThat(template.collection(SkiplistIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(
			template.collection(SkiplistIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
					.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.skiplist));
	}

	public static class PersistentIndexedSingleFieldTestEntity {
		@PersistentIndexed
		private String a;
	}

	@Test
	public void singleFieldPersistentIndexed() {
		assertThat(template.collection(PersistentIndexedSingleFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(
			template.collection(PersistentIndexedSingleFieldTestEntity.class).getIndexes().stream()
					.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.persistent));
		assertThat(template.collection(PersistentIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.persistent).findFirst().get().getFields(),
			hasItems("a"));
	}

	public static class PersistentIndexedMultipleSingleFieldTestEntity {
		@PersistentIndexed
		private String a;
		@PersistentIndexed
		private String b;
	}

	@Test
	public void multipleSingleFieldPersistentIndexed() {
		assertThat(template.collection(PersistentIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(),
			is(3));
		assertThat(
			template.collection(PersistentIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
					.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.persistent));
	}

	@PersistentIndex(fields = { "a" })
	public static class PersistentIndexWithSingleFieldTestEntity {
	}

	@Test
	public void singleFieldPersistentIndex() {
		assertThat(template.collection(PersistentIndexWithSingleFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(
			template.collection(PersistentIndexWithSingleFieldTestEntity.class).getIndexes().stream()
					.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.persistent));
		assertThat(template.collection(PersistentIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.persistent).findFirst().get().getFields(),
			hasItems("a"));
	}

	@PersistentIndex(fields = { "a" })
	@PersistentIndex(fields = { "b" })
	public static class PersistentIndexWithMultipleSingleFieldTestEntity {
	}

	@Test
	public void multipleSingleFieldPersistentIndex() {
		assertThat(template.collection(PersistentIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(),
			is(3));
		assertThat(
			template.collection(PersistentIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
					.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.persistent));
	}

	@PersistentIndex(fields = { "a", "b" })
	public static class PersistentIndexWithMultiFieldTestEntity {
	}

	@Test
	public void multiFieldPersistentIndex() {
		assertThat(template.collection(PersistentIndexWithMultiFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(
			template.collection(PersistentIndexWithMultiFieldTestEntity.class).getIndexes().stream()
					.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.persistent));
		assertThat(
			template.collection(PersistentIndexWithMultiFieldTestEntity.class).getIndexes().stream()
					.filter(i -> i.getType() == IndexType.persistent).findFirst().get().getFields(),
			hasItems("a", "b"));
	}

	@PersistentIndexes({ @PersistentIndex(fields = { "a" }), @PersistentIndex(fields = { "b" }) })
	public static class PersistentIndexWithMultipleIndexesTestEntity {
	}

	@Test
	public void multipleIndexesPersistentIndex() {
		assertThat(template.collection(PersistentIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(),
			is(3));
		assertThat(
			template.collection(PersistentIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
					.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.persistent));
	}

	public static class GeoIndexedSingleFieldTestEntity {
		@GeoIndexed
		private String a;
	}

	@Test
	public void singleFieldGeoIndexed() {
		assertThat(template.collection(GeoIndexedSingleFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(GeoIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.geo1));
		assertThat(template.collection(GeoIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.geo1).findFirst().get().getFields(),
			hasItems("a"));
	}

	public static class GeoIndexedMultipleSingleFieldTestEntity {
		@GeoIndexed
		private String a;
		@GeoIndexed
		private String b;
	}

	@Test
	public void multipleSingleFieldGeoIndexed() {
		assertThat(template.collection(GeoIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(template.collection(GeoIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.geo1));
	}

	@GeoIndex(fields = { "a" })
	public static class GeoIndexWithSingleFieldTestEntity {
	}

	@Test
	public void singleFieldGeoIndex() {
		assertThat(template.collection(GeoIndexWithSingleFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(GeoIndexWithSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.geo1));
		assertThat(template.collection(GeoIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.geo1).findFirst().get().getFields(),
			hasItems("a"));
	}

	@GeoIndex(fields = { "a" })
	@GeoIndex(fields = { "b" })
	public static class GeoIndexWithMultipleSingleFieldTestEntity {
	}

	@Test
	public void multipleSingleFieldGeoIndex() {
		assertThat(template.collection(GeoIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(template.collection(GeoIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.geo1));
	}

	@GeoIndex(fields = { "a", "b" })
	public static class GeoIndexWithMultiFieldTestEntity {
	}

	@Test
	public void multiFieldGeoIndex() {
		assertThat(template.collection(GeoIndexWithMultiFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(GeoIndexWithMultiFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.geo2));
		assertThat(template.collection(GeoIndexWithMultiFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.geo2).findFirst().get().getFields(),
			hasItems("a", "b"));
	}

	@GeoIndexes({ @GeoIndex(fields = { "a" }), @GeoIndex(fields = { "b" }) })
	public static class GeoIndexWithMultipleIndexesTestEntity {
	}

	@Test
	public void multipleIndexesGeoIndex() {
		assertThat(template.collection(GeoIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(template.collection(GeoIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.geo1));
	}

	public static class FulltextIndexedSingleFieldTestEntity {
		@FulltextIndexed
		private String a;
	}

	@Test
	public void singleFieldFulltextIndexed() {
		assertThat(template.collection(FulltextIndexedSingleFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(FulltextIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.fulltext));
		assertThat(template.collection(FulltextIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.fulltext).findFirst().get().getFields(),
			hasItems("a"));
	}

	public static class FulltextIndexedMultipleSingleFieldTestEntity {
		@FulltextIndexed
		private String a;
		@FulltextIndexed
		private String b;
	}

	@Test
	public void multipleSingleFieldFulltextIndexed() {
		assertThat(template.collection(FulltextIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(
			template.collection(FulltextIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
					.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.fulltext));
	}

	@FulltextIndex(field = "a")
	public static class FulltextIndexWithSingleFieldTestEntity {
	}

	@Test
	public void singleFieldFulltextIndex() {
		assertThat(template.collection(FulltextIndexWithSingleFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(
			template.collection(FulltextIndexWithSingleFieldTestEntity.class).getIndexes().stream()
					.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.fulltext));
		assertThat(template.collection(FulltextIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == IndexType.fulltext).findFirst().get().getFields(),
			hasItems("a"));
	}

	@FulltextIndex(field = "a")
	@FulltextIndex(field = "b")
	public static class FulltextIndexWithMultipleSingleFieldTestEntity {
	}

	@Test
	public void multipleSingleFieldFulltextIndex() {
		assertThat(template.collection(FulltextIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(
			template.collection(FulltextIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
					.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.fulltext));
	}

	@FulltextIndexes({ @FulltextIndex(field = "a"), @FulltextIndex(field = "b") })
	public static class FulltextIndexWithMultipleIndexesTestEntity {
	}

	@Test
	public void multipleIndexesFulltextIndex() {
		assertThat(template.collection(FulltextIndexedMultipleSingleFieldTestEntity.class).getIndexes().size(), is(3));
		assertThat(
			template.collection(FulltextIndexedMultipleSingleFieldTestEntity.class).getIndexes().stream()
					.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.fulltext));
	}

	public static class DifferentIndexedAnnotations {
		@HashIndexed
		@SkiplistIndexed
		@PersistentIndexed
		@GeoIndexed
		@FulltextIndexed
		private String a;
	}

	@Test
	public void differentIndexedAnnotationsSameField() {
		assertThat(template.collection(DifferentIndexedAnnotations.class).getIndexes().size(), is(6));
		assertThat(
			template.collection(DifferentIndexedAnnotations.class).getIndexes().stream().map(i -> i.getType())
					.collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash, IndexType.skiplist, IndexType.persistent, IndexType.geo1,
				IndexType.fulltext));
	}

	@HashIndex(fields = { "a" })
	@SkiplistIndex(fields = { "a" })
	@PersistentIndex(fields = { "a" })
	@GeoIndex(fields = { "a" })
	@FulltextIndex(field = "a")
	public static class DifferentIndexAnnotations {

	}

	@Test
	public void differentIndexAnnotations() {
		assertThat(template.collection(DifferentIndexAnnotations.class).getIndexes().size(), is(6));
		assertThat(
			template.collection(DifferentIndexAnnotations.class).getIndexes().stream().map(i -> i.getType())
					.collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash, IndexType.skiplist, IndexType.persistent, IndexType.geo1,
				IndexType.fulltext));
	}

	@HashIndex(fields = { "a" })
	@HashIndex(fields = { "b" })
	@SkiplistIndex(fields = { "a" })
	@SkiplistIndex(fields = { "b" })
	@PersistentIndex(fields = { "a" })
	@PersistentIndex(fields = { "b" })
	@GeoIndex(fields = { "a" })
	@GeoIndex(fields = { "b" })
	@FulltextIndex(field = "a")
	@FulltextIndex(field = "b")
	public static class MultipleDifferentIndexAnnotations {

	}

	@Test
	public void multipleDifferentIndexAnnotations() {
		assertThat(template.collection(MultipleDifferentIndexAnnotations.class).getIndexes().size(), is(11));
		assertThat(
			template.collection(MultipleDifferentIndexAnnotations.class).getIndexes().stream().map(i -> i.getType())
					.collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.hash, IndexType.skiplist, IndexType.persistent, IndexType.geo1,
				IndexType.fulltext));
	}

}
