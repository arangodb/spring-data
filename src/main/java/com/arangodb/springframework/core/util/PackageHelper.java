/**
 * 
 */
package com.arangodb.springframework.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Useful to be able to do a quasi-brute-force scan: i.e., a scan that includes everything but the packages that are known ahead of time to not need to be scanned.
 * An example of the difference this could make: a brute-force scanning could take 2+ seconds, whereas a quasi-brute-force scanning could take .35 seconds (on 
 * an average-capacity development box with a web app coded for deployment on a JEE application server).
 * 
 * @author Reşat SABIQ
 */
public class PackageHelper {
	private static final int PACKAGES_WORTH_SCANNING_ESTIMATED_COLLECTION_SIZE = 5;
	
	private static PackageHelper singleton;
	
	// Additional entries could of course be added going forward (entries only applicable at testing time probably don't need to be included).
	// Have to be alphabetically sorted (improves performance):
	private String[] topLevelPackageArray = {"antlr", "java", "javax", "jdk", "oracle", "sun"};
	private String[] frameworksArray =		{"ch.qos", "org.aopalliance", "org.apache", "org.codehaus", "org.eclipse", "org.hibernate", "org.joda", "org.json", "org.junit", 
			"org.omg", "org.slf4j", "org.springframework", "org.xml"};
	private String[] companiesArray =		{"com.google", "com.ibm", "com.sun"};
	private String[] appServersArray =		{"fish.payara", "org.glassfish", "org.jboss"};
	private String[] dbArray =				{"com.arangodb" };
	private String[][] scanningUnnecessaryArrays = {frameworksArray, companiesArray, appServersArray, dbArray};
	
	public static PackageHelper getInstance() {
		if (singleton == null)
			synchronized (PackageHelper.class) {
				if (singleton == null)
					singleton = new PackageHelper();
			}
		return singleton;
	}

	private PackageHelper() {}
	
	/**
	 * Checks if scanning is necessary for {@code packageStartsWith}.
	 * 
	 * @param packageStartsWith	(Sub)string that a package starts with. Precondition: this method at present expects {@code packageStartsWith} to have 2 levels.
	 * 
	 * @return	false if {@code packageStartsWith} is among packages that this class has been set up to treat as those that scanning is unnecessary for (e.g., 
	 * 				"org.apache"), true otherwise.
	 */
	// Add input pre-processing/validation before making this method public, if that's ever needed
	protected boolean isScanningNecessary(String packageStartsWith) {
		for (String[] array : scanningUnnecessaryArrays) {
			if (Arrays.binarySearch(array, packageStartsWith) >= 0)
				return false;
		}
		return true;
	}

	/**
	 * Checks if scanning is potentially necessary for {@code topLevelPackage}.
	 * 
	 * @param topLevelPackage	Top level package name. Precondition: {@code topLevelPackage} is indeed a top-level package.
	 * 
	 * @return	false if {@code topLevelPackage} is among packages that this class has been set up to treat as those that scanning is unnecessary for (e.g., 
	 * 				"java"), true otherwise.
	 */
	// Add input pre-processing/validation before making this method public, if that's ever needed
	protected boolean isScanningPotentiallyNeccessaryForTopLevelPackage(String topLevelPackage) {
		if (Arrays.binarySearch(topLevelPackageArray, topLevelPackage) >= 0)
			return false;
		return true;
	}

	/**
	 * Provides all packages that it might be worth scanning for entities.
	 * 
	 * @return	All packages except those that this class has been set up to treat as those that scanning is unnecessary for (e.g., 
	 * 				"java", & "org.apache").
	 */
	public List<String> getAllPackagesWorthScanning() {
		Package[] packages = Package.getPackages();
		// Perhaps this could be further micro-optimized to have a smaller memory foot-print than TreeMap in most cases:
		List<String> packagesList = new ArrayList<String>(PACKAGES_WORTH_SCANNING_ESTIMATED_COLLECTION_SIZE);
		for (Package paquet : packages) {
			String name = paquet.getName();
			int topLevelIndex = name.indexOf('.');
			String topLevel = name.substring(0, topLevelIndex);
			if (isScanningPotentiallyNeccessaryForTopLevelPackage(topLevel)) {
				int index = name.indexOf('.', topLevelIndex+1);
				String packageStartsWith = index > -1 ? name.substring(0, index) : name;
				int insertionPoint;
				if (isScanningNecessary(packageStartsWith) && (insertionPoint = Collections.binarySearch(packagesList, packageStartsWith)) < 0)
					packagesList.add((insertionPoint+1)*-1, packageStartsWith);
			}
		}
		return packagesList;
	}
}
