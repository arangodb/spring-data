/**
 * 
 */
package com.arangodb.springframework.core.util;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Reşat SABIQ
 *
 */
public class PackageHelperTest {
	PackageHelper packageHelper = PackageHelper.getInstance();

	/**
	 * Test method for {@link com.arangodb.springframework.core.util.PackageHelper#isScanningPotentiallyNeccessaryForTopLevelPackage(java.lang.String)}.
	 */
	@Test
	public void testIsScanningPotentiallyNeccessaryForTopLevelPackage() {
		Assert.assertTrue(packageHelper.isScanningPotentiallyNeccessaryForTopLevelPackage("custom"));
	}

	/**
	 * Test method for {@link com.arangodb.springframework.core.util.PackageHelper#isScanningNecessary(String)}.
	 */
	@Test 
	public void testIsScanningNecessary() {
		Assert.assertTrue(packageHelper.isScanningNecessary("com.custom"));
	}

	/**
	 * Test method for {@link com.arangodb.springframework.core.util.PackageHelper#isScanningNecessary(String)}.
	 */
	@Test 
	public void scanningUnnecessary() {
		Assert.assertFalse(packageHelper.isScanningNecessary("org.springframework"));
	}

	/**
	 * Test method for {@link com.arangodb.springframework.core.util.PackageHelper#isScanningPotentiallyNeccessaryForTopLevelPackage(java.lang.String)}.
	 */
	@Test
	public void scanningUnneccessaryForTopLevelPackage() {
		Assert.assertFalse(packageHelper.isScanningPotentiallyNeccessaryForTopLevelPackage("javax"));
	}

	/**
	 * Test method for {@link com.arangodb.springframework.core.util.PackageHelper#getAllPackagesWorthScanning()}.
	 */
	@Test
	public void getAllPackagesWorthScanning() {
		List<String> packages = packageHelper.getAllPackagesWorthScanning();
		Assert.assertFalse(packages.contains("java.util"));
	}
}
