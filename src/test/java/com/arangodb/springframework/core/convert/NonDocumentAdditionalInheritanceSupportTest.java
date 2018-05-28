package com.arangodb.springframework.core.convert;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.entity.DocumentEntity;
import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.core.convert.InheritanceSupportTest.PersonSuperClass;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class NonDocumentAdditionalInheritanceSupportTest extends AbstractArangoTest {
	public static class NonDocument extends PersonSuperClass {
		private String extra;
		
		public NonDocument(String name, String extra) {
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
			NonDocument other = (NonDocument) obj;
			if (extra == null) {
				if (other.extra != null)
					return false;
			} else if (!extra.equals(other.extra))
				return false;
			return true;
		}
	}

	@Test
	public void testThatPeopleWhoInsistOnPersistingNonDocumentsAsDocumentsCanDoSo() {
		NonDocument orig = new NonDocument("Twisted", "strange");
		final DocumentEntity ref = template.insert(orig);
		final NonDocument entity = template.find(ref.getId(), NonDocument.class).get();
		
		assertThat(entity, is(notNullValue()));
		assertThat(entity.getId(), is(ref.getId()));
		assertTrue(entity.equals(orig));
	}
}
