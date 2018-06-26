/**
 * 
 */
package com.arangodb.springframework.core.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * @author Reşat SABIQ
 */
public class MetadataUtilsTest {
	private static final String COLLECTION_NAME = "person";
	private static final String NUMERIC_ID = "15583087";
	
	/**
	 * Test method for {@link com.arangodb.springframework.core.util.MetadataUtils#determineDocumentKeyFromId(java.lang.String)}.
	 */
	@Test
	public void testDetermineDocumentKeyFromId() {
		String key = MetadataUtils.determineDocumentKeyFromId(COLLECTION_NAME+ '/' +NUMERIC_ID);
		assertThat(key, is(NUMERIC_ID));
	}
}
