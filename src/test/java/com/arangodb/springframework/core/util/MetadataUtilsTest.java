/**
 * 
 */
package com.arangodb.springframework.core.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

/**
 * @author Reşat SABIQ
 */
public class MetadataUtilsTest {
	private static final String COLLECTION_NAME = "person";
	private static final String KEY = "15583087";
	
	/**
	 * Test method for {@link com.arangodb.springframework.core.util.MetadataUtils#determineDocumentKeyFromId(java.lang.String)}.
	 */
	@Test
	public void testDetermineDocumentKeyFromId() {
		String key = MetadataUtils.determineDocumentKeyFromId(COLLECTION_NAME+ MetadataUtils.KEY_DELIMITER +KEY);
		assertThat(key, is(KEY));
	}

	/**
	 * Test method for {@link com.arangodb.springframework.core.util.MetadataUtils#determineCollectionFromId(java.lang.String)}.
	 */
	@Test
	public void testDetermineCollectionFromId() {
		String c = MetadataUtils.determineCollectionFromId(COLLECTION_NAME+ MetadataUtils.KEY_DELIMITER +KEY);
		assertThat(c, is(COLLECTION_NAME));
	}
}
