/**
 * 
 */
package com.arangodb.springframework.core.util;

/**
 * Generic utilities for metadata, such as records' _keys.
 * 
 * @author Reşat SABIQ
 */
public class MetadataUtils {
	public static final char KEY_DELIMITER = '/';

	private MetadataUtils() {}
	
	/**
	 * Provides a substring with _key.
	 * @param id	string consisting of concatenation of collection name, /, & _key.
	 * @return	_key
	 */
	public static String determineDocumentKeyFromId(final String id) {
		final int lastSlash = id.lastIndexOf(KEY_DELIMITER);
		return id.substring(lastSlash+1);
	}
	
	/**
	 * Provides a substring with collection name.
	 * @param id	string consisting of concatenation of collection name, /, & _key.
	 * @return	collection name (or null if no key delimiter is present)
	 */
	public static String determineCollectionFromId(final String id) {
		final int delimiter = id.indexOf(KEY_DELIMITER);
		return delimiter == -1 ? null : id.substring(0, delimiter);
	}
}
