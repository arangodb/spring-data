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
	public static final char ID_DELIMITER = '/';

	private MetadataUtils() {}
	
	/**
	 * Provides a substring with _key.
	 * @param id	string consisting of concatenation of collection name, /, & _key.
	 * @return	_key
	 */
	public static String determineDocumentKeyFromId(final String id) {
		final int lastSlash = id.lastIndexOf(ID_DELIMITER);
		return id.substring(lastSlash+1);
	}
}
