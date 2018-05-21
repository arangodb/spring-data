/**
 * 
 */
package com.arangodb.springframework.core.convert;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.entity.DocumentEntity;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Ref;

/**
 * @author Reşat SABIQ
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class InheritanceSupportTest extends AbstractArangoTest {
	@Document
	public static abstract class Base {
		@Id
		private String id;

		public String getId() {
			return id;
		}
	}
	@Document
	public static class PersonParent extends Base {
		private String name;
		
		public PersonParent(String name) {
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
			PersonParent other = (PersonParent) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}
	@Document
	public static class DeveloperChild extends PersonParent {
		private String mainDevelopmentSkill;

		public DeveloperChild(String name, String mainDevelopmentSkill) {
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
			DeveloperChild other = (DeveloperChild) obj;
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
		private PersonParent developerChild;

		public Aggregate(PersonParent developerChild) {
			super();
			this.developerChild = developerChild;
		}

		public PersonParent getPersonParent() {
			return developerChild;
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
			if (developerChild == null) {
				if (other.developerChild != null)
					return false;
			} else if (!developerChild.equals(other.developerChild))
				return false;
			return true;
		}
	}
	@Document
	public static class AggregateWithCollection extends Base {
		@Ref(lazy=false)
		private Collection<PersonParent> personsAndDevelopers;

		public AggregateWithCollection(Collection<PersonParent> personsAndDevelopers) {
			this.personsAndDevelopers = personsAndDevelopers;
		}

		public Collection<PersonParent> getPersonsAndDevelopers() {
			return personsAndDevelopers;
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
			if (personsAndDevelopers == null) {
				if (other.personsAndDevelopers != null)
					return false;
			} else if (!personsAndDevelopers.equals(other.personsAndDevelopers))
				return false;
			return true;
		}
	}

	@Test
	public void coreTablePerClassTypeInheritanceSupport() {
		PersonParent child = new DeveloperChild("Reşat", "Java");
		Aggregate orig = new Aggregate(child);
		template.insert(child);
		final DocumentEntity ref = template.insert(orig);
		final Aggregate entity = template.find(ref.getId(), Aggregate.class).get();
		assertThat(entity, is(notNullValue()));
		assertTrue("Subclass should be auto-retrieved after it had been persisted", entity.getPersonParent().getClass().equals(orig.getPersonParent().getClass()));
		assertThat(entity.getId(), is(ref.getId()));
		assertTrue(entity.equals(orig));
	}
	
	@Test
	public void coreTablePerClassTypeInheritanceSupportForCollections() {
		PersonParent child = new DeveloperChild("Reşat", "Java");
		PersonParent child2 = new PersonParent("İsxaq");
		List<PersonParent> children = new ArrayList<PersonParent>();
		children.add(child);
		children.add(child2);
		AggregateWithCollection orig = new AggregateWithCollection(children);
		template.insert(child);
		template.insert(child2);
		final DocumentEntity ref = template.insert(orig);
		final AggregateWithCollection entity = template.find(ref.getId(), AggregateWithCollection.class).get();
		assertThat(entity, is(notNullValue()));
		Collection<PersonParent> retrievedChildren = entity.getPersonsAndDevelopers();
		int subclassCount = 0, parentClassCount = 0;
		for (PersonParent c : retrievedChildren) {
			if (c instanceof DeveloperChild)
				subclassCount++;
			else
				parentClassCount++;
		}
		assertTrue(subclassCount == 1);
		assertTrue(parentClassCount == 1);
		assertThat(entity.getId(), is(ref.getId()));
		assertTrue(entity.equals(orig));
	}
}
