/**
 * 
 */
package com.arangodb.springframework.core.util;

import java.util.Set;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.util.Pair;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.config.ArangoEntityClassScanner;
import com.arangodb.springframework.core.mapping.ArangoPersistentEntity;
import com.arangodb.springframework.core.mapping.ArangoPersistentProperty;

/**
 * Utilities to facilitate support for inheritance in persisted entities.
 * 
 * @author Reşat SABIQ
 */
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
	 * @param source	Type-containing reference id.
	 * @param property	Reference property.
	 * @param context	Mapping context to check while matching.
	 * 
	 * @return	A pair, where first property is the (sub)type best matching {@code source}, & second property is true when there had been a persistent entity
	 * 			in {@code mappingContextPersistentEntities} for it (& false otherwise).
	 */
	public static Pair<Class<?>, Boolean> determineInheritanceAwareReferenceType(
		final Object source,
		final ArangoPersistentProperty property,
		final MappingContext<? extends ArangoPersistentEntity<?>, ArangoPersistentProperty> context) {
		// Written as part of basic initial fix for TABLE_PER_CLASS type inheritance support:
		Class<?> type = null;
		String src = source.toString();
		String entityName = src.substring(0, src.indexOf('/'));
		boolean inContext = false;
		// At present, a subclass would quite likely have a simple class name reflected in Id that is different from the property's (compile-time) type name (ignoring case):
		handleInheritance:
		if (!verifyMatch(property.getType(), entityName)) {
			// Find the matching entity in the context:
			for (PersistentEntity<?,?> entityNode : context.getPersistentEntities()) {
				if (verifyMatch(entityNode.getType(), entityName)) {
					// This is the type of the subclass that's needed for inheritance support:
					type = entityNode.getType();
					inContext = true;
					break handleInheritance;
				}
			}
			
			/* It's not in context: search step-by-step so that we stop sooner rather than later: */
			// 1. Check same & child packages (sub-classes would most likely be found this way)
			Package samePackage = property.getType().getPackage();
			type = findAssignablesReflectively(samePackage.getName(), entityName, property);
			if (type == null) {
				String parentPackage = samePackage.getName().substring(0, samePackage.getName().lastIndexOf('.'));
				// 2. Check parent & sibling packages (& sub-packages)
				type = findAssignablesReflectively(parentPackage, entityName, property);
				if (type == null) {
					// 3. Quasi-brute-force:
					if (QUASI_BRUTE_FORCE_SCANNING_INSTEAD_OF_EXCEPTION_4_INHERITANCE_SUPPORT) {
						Set<String> tldSet = packageHelper.getAllPackagesWorthScanning();
						for (String tld : tldSet) {
							type = findAssignablesReflectively(tld, entityName, property);
							if (type != null)
								break;
						}
					} else // CHECKME: Consider whether to throw an exception like the one below instead of quasi-brute-force scanning (which could take .3 seconds or so)
						throw new IllegalStateException("Please add the package for \"" +entityName+ "\" to the list of base packages to be scanned when configuring ArangoDB spring-data");

				}
			}
		}
		if (type == null) // no subclass involved
			type = property.getTypeInformation().getType();
		return Pair.of(type, inContext ? Boolean.TRUE : Boolean.FALSE);
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
			final ArangoPersistentProperty property) {
		try {
			Set<Class<?>> subTypes = ArangoEntityClassScanner.scanForEntities(property.getType(), basePackage);
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
		return (doc != null && entityName.equals(doc.value())) || entityName.equals(candidateType.getSimpleName().toLowerCase());
	}
}
