/**
 * 
 */
package com.arangodb.springframework.core.convert;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
	public static class Base {
		@Id
		private String id;

		public String getId() {
			return id;
		}
	}
	@Document
	public static class Parent extends Base {
		private String one;
		
		public Parent(String one) {
			super();
			this.one = one;
		}

		public String getOne() {
			return one;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Parent other = (Parent) obj;
			if (one == null) {
				if (other.one != null)
					return false;
			} else if (!one.equals(other.one))
				return false;
			return true;
		}
	}
	@Document
	public static class Child extends Parent {
		private String two;

		public Child(String one, String two) {
			super(one);
			this.two = two;
		}

		public String getTwo() {
			return two;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			Child other = (Child) obj;
			if (two == null) {
				if (other.two != null)
					return false;
			} else if (!two.equals(other.two))
				return false;
			return true;
		}
	}
	@Document
	public static class Aggregate extends Base {
		@Ref(lazy=false)
		private Parent child;

		public Aggregate(Parent child) {
			super();
			this.child = child;
		}

		public Parent getParent() {
			return child;
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
			if (child == null) {
				if (other.child != null)
					return false;
			} else if (!child.equals(other.child))
				return false;
			return true;
		}
	}

	@Test
	public void coreTablePerClassTypeInheritanceSupport() {
		Parent child = new Child("one", "two");
		Aggregate orig = new Aggregate(child);
		template.insert(child);
		final DocumentEntity ref = template.insert(orig);
		final Aggregate entity = template.find(ref.getId(), Aggregate.class).get();
		assertThat(entity, is(notNullValue()));
		assertTrue("Subclass should be auto-retrieved after it had been persisted", entity.getParent().getClass().equals(orig.getParent().getClass()));
		assertThat(entity.getId(), is(ref.getId()));
		assertTrue(entity.equals(orig));
	}
}
