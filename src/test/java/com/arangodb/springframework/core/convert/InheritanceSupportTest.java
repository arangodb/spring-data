/**
 * 
 */
package com.arangodb.springframework.core.convert;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.entity.DocumentEntity;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Field;
import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.repository.InheritanceSupportTestRepository;

/**
 * @author Reşat SABIQ
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class InheritanceSupportTest extends AbstractArangoTest {
	@Autowired
	InheritanceSupportTestRepository inheritanceSupportRepository;
	
	public static abstract class Base {
		@Id
		private String id;

		public String getId() {
			return id;
		}
	}

	@Document("person")
	public static class PersonSuperClass extends Base {
		private String name;
		
		public PersonSuperClass(String name) {
			super();
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;

			PersonSuperClass other = (PersonSuperClass) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}

	@Document("developer")
	public static class DeveloperSubclass extends PersonSuperClass {
		@Field("mainSkill")
		private String mainDevelopmentSkill;

		public DeveloperSubclass(String name, String mainDevelopmentSkill) {
			super(name);
			this.mainDevelopmentSkill = mainDevelopmentSkill;
		}

		public String getMainDevelopmentSkill() {
			return mainDevelopmentSkill;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;

			DeveloperSubclass other = (DeveloperSubclass) obj;
			if (mainDevelopmentSkill == null) {
				if (other.mainDevelopmentSkill != null)
					return false;
			} else if (!mainDevelopmentSkill.equals(other.mainDevelopmentSkill))
				return false;
			return true;
		}
	}
	
	@Document
	public static class Aggregate extends Base {
		@Ref(lazy=false)
		private PersonSuperClass developerSubclassInstance;

		public Aggregate() {
		}
		
		public Aggregate(PersonSuperClass developerSubclassInstance) {
			super();
			this.developerSubclassInstance = developerSubclassInstance;
		}

		public PersonSuperClass getPerson() {
			return developerSubclassInstance;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Aggregate other = (Aggregate) obj;
			
			if (developerSubclassInstance == null) {
				if (other.developerSubclassInstance != null)
					return false;
			} else if (!developerSubclassInstance.equals(other.developerSubclassInstance))
				return false;
			return true;
		}
	}
	
	@Document
	public static class AggregateWithCollection extends Base {
		@Ref(lazy=false)
		private Collection<PersonSuperClass> personsIncludingDevelopers;

		public AggregateWithCollection() {
		}
		public AggregateWithCollection(Collection<PersonSuperClass> personsAndDevelopers) {
			this.personsIncludingDevelopers = personsAndDevelopers;
		}

		public Collection<PersonSuperClass> getPersonsIncludingDevelopers() {
			return personsIncludingDevelopers;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AggregateWithCollection other = (AggregateWithCollection) obj;

			if (personsIncludingDevelopers == null) {
				if (other.personsIncludingDevelopers != null)
					return false;
			} else if (!personsIncludingDevelopers.equals(other.personsIncludingDevelopers))
				return false;
			return true;
		}
	}
	
	@Test
	public void coreCollectionPerClassTypeInheritanceSupport() {
		PersonSuperClass subclassInstance = new DeveloperSubclass("Reşat", "Java");
		Aggregate orig = new Aggregate(subclassInstance);
		template.insert(subclassInstance);
		final DocumentEntity ref = template.insert(orig);
		final Aggregate entity = template.find(ref.getId(), Aggregate.class).get();
		
		assertThat(entity, is(notNullValue()));
		assertTrue("Subclass should be auto-retrieved after it had been persisted", entity.getPerson().getClass().equals(orig.getPerson().getClass()));
		assertThat(entity.getId(), is(ref.getId()));
		assertTrue(entity.equals(orig));
	}
	
	@Test
	public void collectionPerClassTypeInheritanceSupportForCollections() {
		PersonSuperClass subclassInstance = new DeveloperSubclass("Reşat", "Java");
		PersonSuperClass superClassInstance = new PersonSuperClass("İsxaq");
		List<PersonSuperClass> instances = new ArrayList<PersonSuperClass>();
		instances.add(subclassInstance);
		instances.add(superClassInstance);
		AggregateWithCollection orig = new AggregateWithCollection(instances);
		template.insert(subclassInstance);
		template.insert(superClassInstance);
		final DocumentEntity ref = template.insert(orig);
		final AggregateWithCollection entity = template.find(ref.getId(), AggregateWithCollection.class).get();
		assertThat(entity, is(notNullValue()));
		Collection<PersonSuperClass> retrievedChildren = entity.getPersonsIncludingDevelopers();
		int subclassCount = 0, parentClassCount = 0;
		for (PersonSuperClass c : retrievedChildren) {
			if (c instanceof DeveloperSubclass)
				subclassCount++;
			else
				parentClassCount++;
		}
		assertTrue(subclassCount == 1);
		assertTrue(parentClassCount == 1);
		assertThat(entity.getId(), is(ref.getId()));
		assertTrue(entity.equals(orig));
	}

	/**
	 * This tests that unnecessary data is not persisted for TABLE/COLLECTION_PER_CLASS type inheritance (as has been recently introduced by a merge).
	 */
	@Test
	public void ensureThatCollectionPerClassTypeMainstreamInheritanceImplementationIsOptimal() {
		PersonSuperClass person = new PersonSuperClass("Reşat");
		template.insert(person);
		final Map<String, Object> retrieved = inheritanceSupportRepository.findOne(person.getId());
		
		assertFalse("There is no need for storing type data as a property/column for TABLE/COLLECTION_PER_CLASS type of inheritance", 
				retrieved.containsKey(DefaultArangoTypeMapper.DEFAULT_TYPE_KEY));
	}
}
