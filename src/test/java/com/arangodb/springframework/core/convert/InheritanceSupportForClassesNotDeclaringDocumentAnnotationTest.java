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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class InheritanceSupportForClassesNotDeclaringDocumentAnnotationTest extends AbstractArangoTest {
	@Document("parents")
	public static class Parent {
		@Id
		private String id;
		private String name;
		
		public Parent(String name) {
			super();
			this.name = name;
		}

		public String getId() {
			return id;
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

			Parent other = (Parent) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}
	
	public static class Child extends Parent {
		private String extra;
		
		public Child(String name, String extra) {
			super(name);
			this.extra = extra;
		}

		public String getExtra() {
			return extra;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			Child other = (Child) obj;
			if (extra == null) {
				if (other.extra != null)
					return false;
			} else if (!extra.equals(other.extra))
				return false;
			return true;
		}
	}

	@Test
	public void itIsPossibleToPersistAClassThatDoesNotHaveADeclaredDocumentAnnotation() {
		Child orig = new Child("Excessive", "flexibility");
		final DocumentEntity ref = template.insert(orig);
		final Child entity = template.find(ref.getId(), Child.class).get();
		
		assertThat(entity, is(notNullValue()));
		assertThat(entity.getId(), is(ref.getId()));
		assertTrue(entity.equals(orig));
	}
}
