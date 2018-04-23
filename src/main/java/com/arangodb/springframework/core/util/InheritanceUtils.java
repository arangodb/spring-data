/**
 * 
 */
package com.arangodb.springframework.core.util;

import java.util.List;
import java.util.Set;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.config.ArangoEntityClassScanner;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;

/**
 * Utilities to facilitate support for inheritance in persisted entities.
 * 
 * @author Reşat SABIQ
 */
// Originally written as part of fix for TABLE/COLLECTION_PER_CLASS type inheritance support:
// This approach is superior to was merged after this push request (18) was submitted as part of pull request 33, because that pull request stores fully-qualified 
// class name for each record with inheritance which is completely unnecessary for TABLE/COLLECTION_PER_CLASS type inheritance because there is already an entire 
// TABLE/COLLECTION dedicated for the class involved (thus, this approach optimizes disk space, memory, bandwidth & CPU usage by avoiding the unnecessary overhead 
// entailed by dealing with unnecessary data).
public class InheritanceUtils {
	// (Something like) this could even be made configurable (via arangodb.properties):
	private static final boolean QUASI_BRUTE_FORCE_SCANNING_INSTEAD_OF_EXCEPTION_4_INHERITANCE_SUPPORT = true;
	private static final PackageHelper packageHelper = PackageHelper.getInstance();
	
	private InheritanceUtils() {}
	
	/**
	 * Finds the (sub)type best matching the reference Id containing the actual type name. 
	 * The (sub-)type found has the same class name as that contained in {@code source},
	 * & is of the same type as or extends the type of {@code property}.
	 * 
	 * @param source		Type-containing reference id.
	 * @param propertyType	Reference property type.
	 * @param context		Mapping context to check while matching.
	 * 
	 * @return	A pair, where first property is the (sub)type best matching {@code source}, & second property is true when there had been a persistent entity
	 * 			in {@code mappingContextPersistentEntities} for it (& false otherwise).
	 */
	public static Class<?> determineInheritanceAwareReferenceType(
		final Object source,
		final Class<?> propertyType,
		final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context) {
		Class<?> type = null;
		String src = source.toString();
		String entityName = src.substring(0, src.indexOf('/'));
		// At present, a subclass would quite likely have a simple class name reflected in Id that is different from the property's (compile-time) type name (ignoring case):
		handleInheritance:
		if (!verifyMatch(propertyType, entityName)) {
			// Find the matching entity in the context:
			for (PersistentEntity<?,?> entityNode : context.getPersistentEntities()) {
				if (verifyMatch(entityNode.getType(), entityName)) {
					// This is the type of the subclass that's needed for inheritance support:
					type = entityNode.getType();
					break handleInheritance;
				}
			}
			
			/* It's not in context: search step-by-step so that we stop sooner rather than later: */
			// 1. Check same & child packages (sub-classes would most likely be found this way)
			Package samePackage = propertyType.getPackage();
			type = findAssignablesReflectively(samePackage.getName(), entityName, propertyType);
			if (type == null) {
				String parentPackage = samePackage.getName().substring(0, samePackage.getName().lastIndexOf('.'));
				// 2. Check parent & sibling packages (& sub-packages)
				type = findAssignablesReflectively(parentPackage, entityName, propertyType);
				if (type == null) {
					// 3. Quasi-brute-force:
					if (QUASI_BRUTE_FORCE_SCANNING_INSTEAD_OF_EXCEPTION_4_INHERITANCE_SUPPORT) {
						List<String> packagesWorthScanning = packageHelper.getAllPackagesWorthScanning();
						for (String tld : packagesWorthScanning) {
							type = findAssignablesReflectively(tld, entityName, propertyType);
							if (type != null)
								break;
						}
					} else // CHECKME: Consider whether to throw an exception like the one below instead of quasi-brute-force scanning (which could take .3 seconds or so)
						throw new IllegalStateException("Please add the package for \"" +entityName+ "\" to the list of base packages to be scanned when configuring ArangoDB spring-data");

				}
			}
		}
		if (type == null) // no subclass involved
			type = propertyType;
		return type; // This would be a bit lighter on CPU, but would involve a bit more GC: Pair.of(type, inContext ? Boolean.TRUE : Boolean.FALSE);
	}

	/**
	 * Reflectively finds match assignable to {@code property}'s type.
	 * 
	 * @param basePackage	package to start searching under.
	 * @param entityName	entity name to search for.
	 * @param property		property to whose type the matches must be assignable.
	 * 
	 * @return	valid match or null.
	 */
	private static Class<?> findAssignablesReflectively(
			final String basePackage, 
			final String entityName,
			final Class<?> propertyType) {
		try {
			Set<Class<?>> subTypes = ArangoEntityClassScanner.scanForEntities(propertyType, basePackage);
//			if (subTypes != null) // doesn't return null
				for (Class<?> klass : subTypes)
					if (verifyMatch(klass, entityName))
						return klass;
		} catch (ClassNotFoundException e) { // shouldn't happen in real-world
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Verifies that {@code candidateType} is a {@link Document}, whose explicit or implicit document/entity name matches {@code entityName}.
	 * 
	 * @param candidateType	candidate type to verify.
	 * @param entityName	entity name to match.
	 * 
	 * @return	true if valid match, false otherwise.
	 */
	private static boolean verifyMatch(final Class<?> candidateType, final String entityName) {
		Document doc = candidateType.getAnnotation(Document.class);
		// First compare with explicit (annotation) name, then with implicit (class) name:
		if (doc != null && entityName.equals(doc.value()))
			return true;
		String name = candidateType.getSimpleName();
		name = name.substring(0, 1).toLowerCase() + name.substring(1);
		return entityName.equals(name);
	}
}
