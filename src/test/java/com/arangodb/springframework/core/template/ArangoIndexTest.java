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

package com.arangodb.springframework.core.template;

import com.arangodb.entity.IndexEntity;
import com.arangodb.entity.IndexType;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.FulltextIndex;
import com.arangodb.springframework.annotation.FulltextIndexed;
import com.arangodb.springframework.annotation.FulltextIndexes;
import com.arangodb.springframework.annotation.GeoIndex;
import com.arangodb.springframework.annotation.GeoIndexed;
import com.arangodb.springframework.annotation.GeoIndexes;
import com.arangodb.springframework.annotation.PersistentIndex;
import com.arangodb.springframework.annotation.PersistentIndexed;
import com.arangodb.springframework.annotation.PersistentIndexes;
import com.arangodb.springframework.annotation.TtlIndex;
import com.arangodb.springframework.annotation.TtlIndexed;
import com.arangodb.springframework.core.geo.GeoJsonPoint;
import org.junit.Test;
import org.springframework.data.mapping.MappingException;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoIndexTest extends AbstractArangoTest {

	private IndexType geo1() {
		return geoType(IndexType.geo1);
	}

	private IndexType geo2() {
		return geoType(IndexType.geo2);
	}

	private IndexType geoType(final IndexType type) {
		return Integer.valueOf(template.getVersion().getVersion().split("\\.")[1]) >= 4 ? IndexType.geo : type;
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
				hasItems(IndexType.primary, geo1()));
		assertThat(template.collection(GeoIndexedSingleFieldTestEntity.class).getIndexes().stream()
						.filter(i -> i.getType() == geo1()).findFirst().get().getFields(),
				hasItems("a"));
	}

	public static class GeoIndexedGeoJsonFieldTestEntity {
		@GeoIndexed(geoJson = true)
		private GeoJsonPoint a;
	}

	@Test
	public void geoJsonFieldGeoIndexed() {
		assertThat(template.collection(GeoIndexedGeoJsonFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(GeoIndexedGeoJsonFieldTestEntity.class).getIndexes().stream()
						.map(i -> i.getType()).collect(Collectors.toList()),
				hasItems(IndexType.primary, geo1()));
		assertThat(template.collection(GeoIndexedGeoJsonFieldTestEntity.class).getIndexes().stream()
						.filter(i -> i.getType() == geo1()).findFirst().get().getFields(),
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
			hasItems(IndexType.primary, geo1()));
	}

	@GeoIndex(fields = { "a" })
	public static class GeoIndexWithSingleFieldTestEntity {
	}

	@Test
	public void singleFieldGeoIndex() {
		assertThat(template.collection(GeoIndexWithSingleFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(GeoIndexWithSingleFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, geo1()));
		assertThat(template.collection(GeoIndexedSingleFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == geo1()).findFirst().get().getFields(),
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
			hasItems(IndexType.primary, geo1()));
	}

	@GeoIndex(fields = { "a", "b" })
	public static class GeoIndexWithMultiFieldTestEntity {
	}

	@Test
	public void multiFieldGeoIndex() {
		assertThat(template.collection(GeoIndexWithMultiFieldTestEntity.class).getIndexes().size(), is(2));
		assertThat(template.collection(GeoIndexWithMultiFieldTestEntity.class).getIndexes().stream()
				.map(i -> i.getType()).collect(Collectors.toList()),
			hasItems(IndexType.primary, geo2()));
		assertThat(template.collection(GeoIndexWithMultiFieldTestEntity.class).getIndexes().stream()
				.filter(i -> i.getType() == geo2()).findFirst().get().getFields(),
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
			hasItems(IndexType.primary, geo1()));
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
		@PersistentIndexed
		@GeoIndexed
		@FulltextIndexed
		@TtlIndexed
		private String a;
	}

	@Test
	public void differentIndexedAnnotationsSameField() {
		assertThat(template.collection(DifferentIndexedAnnotations.class).getIndexes().size(), is(5));
		assertThat(
			template.collection(DifferentIndexedAnnotations.class).getIndexes().stream().map(i -> i.getType())
					.collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.persistent, geo1(), IndexType.fulltext, IndexType.ttl));
	}

	@PersistentIndex(fields = { "a" })
	@GeoIndex(fields = { "a" })
	@FulltextIndex(field = "a")
	public static class DifferentIndexAnnotations {

	}

	@Test
	public void differentIndexAnnotations() {
		assertThat(template.collection(DifferentIndexAnnotations.class).getIndexes().size(), is(4));
		assertThat(
			template.collection(DifferentIndexAnnotations.class).getIndexes().stream().map(i -> i.getType())
					.collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.persistent, geo1(), IndexType.fulltext));
	}

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
		assertThat(template.collection(MultipleDifferentIndexAnnotations.class).getIndexes().size(), is(7));
		assertThat(
			template.collection(MultipleDifferentIndexAnnotations.class).getIndexes().stream().map(i -> i.getType())
					.collect(Collectors.toList()),
			hasItems(IndexType.primary, IndexType.persistent, geo1(),
				IndexType.fulltext));
	}

	@Document("TwoEntityCollectionWithAdditionalIndexesCollection")
	@PersistentIndex(fields = "a")
	static class TwoEntityCollectionWithAdditionalIndexesTestEntity1 {

	}

	@Document("TwoEntityCollectionWithAdditionalIndexesCollection")
	@PersistentIndex(fields = {"a", "b"})
	static class TwoEntityCollectionWithAdditionalIndexesTestEntity2 {

	}

	@Test
	public void twoEntityCollectionWithAdditionalIndexes() {
		// one primary + one persistent index
		assertThat(template.collection(TwoEntityCollectionWithAdditionalIndexesTestEntity1.class).getIndexes().size(),
			is(1 + 1));
		// one primary + two persistent index
		assertThat(template.collection(TwoEntityCollectionWithAdditionalIndexesTestEntity2.class).getIndexes().size(),
			is(1 + 2));
		// one primary + two persistent index
		assertThat(template.collection(TwoEntityCollectionWithAdditionalIndexesTestEntity1.class).getIndexes().size(),
			is(1 + 2));
	}

	public static class TtlIndexedSingleFieldTestEntity {
		@TtlIndexed
		private String a;
	}

	@Test
	public void singleFieldTtlIndexed() {
		Collection<IndexEntity> indexes = template.collection(TtlIndexedSingleFieldTestEntity.class).getIndexes();
		assertThat(indexes, hasSize(2));
		assertThat(indexes.stream().map(IndexEntity::getType).collect(Collectors.toList()),
				hasItems(IndexType.primary, IndexType.ttl));
		IndexEntity ttlIdx = indexes.stream().filter(i -> i.getType() == IndexType.ttl).findFirst().get();
		assertThat(ttlIdx.getFields(), hasSize(1));
		assertThat(ttlIdx.getFields(), hasItems("a"));
		assertThat(ttlIdx.getExpireAfter(), is(0));
	}

	public static class TtlIndexedExpireAfterTestEntity {
		@TtlIndexed(expireAfter = 3600)
		private String a;
	}

	@Test
	public void expireAfterTtlIndexed() {
		Collection<IndexEntity> indexes = template.collection(TtlIndexedExpireAfterTestEntity.class).getIndexes();
		assertThat(indexes, hasSize(2));
		assertThat(indexes.stream().map(IndexEntity::getType).collect(Collectors.toList()),
				hasItems(IndexType.primary, IndexType.ttl));
		IndexEntity ttlIdx = indexes.stream().filter(i -> i.getType() == IndexType.ttl).findFirst().get();
		assertThat(ttlIdx.getFields(), hasSize(1));
		assertThat(ttlIdx.getFields(), hasItems("a"));
		assertThat(ttlIdx.getExpireAfter(), is(3600));
	}

	public static class MultipleTtlIndexedTestEntity {
		@TtlIndexed
		private String a;

		@TtlIndexed
		private String b;
	}

	@Test
	public void multipleTtlIndexedShouldThrow() {
		try {
			template.collection(MultipleTtlIndexedTestEntity.class).getIndexes();
			fail("did not throw");
		} catch (MappingException e) {
			assertThat(e.getMessage(), containsString("Found multiple ttl indexed properties!"));
		}
	}

	@TtlIndex(field = "a",expireAfter = 3600)
	public static class TtlIndexTestEntity {
	}

	@Test
	public void ttlIndex() {
		Collection<IndexEntity> indexes = template.collection(TtlIndexTestEntity.class).getIndexes();
		assertThat(indexes, hasSize(2));
		assertThat(indexes.stream().map(IndexEntity::getType).collect(Collectors.toList()),
				hasItems(IndexType.primary, IndexType.ttl));
		IndexEntity ttlIdx = indexes.stream().filter(i -> i.getType() == IndexType.ttl).findFirst().get();
		assertThat(ttlIdx.getFields(), hasSize(1));
		assertThat(ttlIdx.getFields(), hasItems("a"));
		assertThat(ttlIdx.getExpireAfter(), is(3600));
	}

}
