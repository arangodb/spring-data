package com.arangodb.springframework.repository.query.graphtraversal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.ArangoTestConfiguration;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.repository.HumanBeingRepository;
import com.arangodb.springframework.testdata.ChildOf;
import com.arangodb.springframework.testdata.HumanBeing;

/**
 * Tests in-bound, & out-bound graph traversals for the following graph (level shown in parantheses):
 *     Sansa(1)
 *    /    \
 * Ned(0)  Catelyn(0)  Jon(0)   Jaimie(0)
 *    \    /              \    /
 *     Robb(1)             Emily(1)
 *         \              /
 *          \            /
 *           \          /
 *            \        /
 *              Dude(2)
 *                 |
 *             Dudette(3)
 * 
 * @author Reşat SABIQ
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class HumanBeingGraphTraversalTest extends AbstractArangoTest {
	protected static final Class<?>[] COLLECTIONS = new Class<?>[] { HumanBeing.class, ChildOf.class };
	private static final boolean DO_SYSOUT = false;
	
	@Autowired
	private HumanBeingRepository characterRepo;
	@Autowired
	protected ArangoOperations template;

	public static Collection<HumanBeing> makeCharacters() {
		return Arrays.asList(new HumanBeing("Ned", "Stark", false, 61), new HumanBeing("Catelyn", "Stark", false, 60),
				new HumanBeing("Emily", "Snow", true, 40), new HumanBeing("Dude", "Stark", true, 20), 
				new HumanBeing("Dudette", "Stark", true, 2), new HumanBeing("Sansa", "Stark", true, 13), 
				new HumanBeing("Robb", "Stark", false, 40), new HumanBeing("Jon", "Snow", true, 16), 
				new HumanBeing("Jaimie", "Lanister", true, 36)
		);
	}

	@Before
	public void setUp() throws Exception {
		for (final Class<?> collection : COLLECTIONS)
			template.collection(collection).drop();
		populateData();
	}

	private void populateData() {
		characterRepo.saveAll(makeCharacters());
		
		characterRepo.findByNameAndSurname("Ned", "Stark").ifPresent(ned -> { // Requires Administrate permission on _system
			characterRepo.findByNameAndSurname("Catelyn", "Stark").ifPresent(catelyn -> {
				characterRepo.findByNameAndSurname("Robb", "Stark").ifPresent(robb -> {
					template.insert(Arrays.asList(new ChildOf(robb, ned), new ChildOf(robb, catelyn)), ChildOf.class);
				});
				characterRepo.findByNameAndSurname("Sansa", "Stark").ifPresent(sansa -> {
					template.insert(Arrays.asList(new ChildOf(sansa, ned), new ChildOf(sansa, catelyn)), ChildOf.class);
				});
			});
		});
		characterRepo.findByNameAndSurname("Jon", "Snow").ifPresent(jon -> {
			characterRepo.findByNameAndSurname("Jaimie", "Lanister").ifPresent(jaimie -> {
				characterRepo.findByNameAndSurname("Emily", "Snow").ifPresent(emily -> {
					template.insert(Arrays.asList(new ChildOf(emily, jon), new ChildOf(emily, jaimie)), ChildOf.class);
				});
			});
		});

		characterRepo.findByNameAndSurname("Robb", "Stark").ifPresent(robb -> {
			characterRepo.findByNameAndSurname("Emily", "Snow").ifPresent(emily-> {
				characterRepo.findByNameAndSurname("Dude", "Stark").ifPresent(dude-> {
					template.insert(Arrays.asList(new ChildOf(dude, robb), new ChildOf(dude, emily)), ChildOf.class);
					characterRepo.findByNameAndSurname("Dudette", "Stark").ifPresent(dudette-> {
						template.insert(new ChildOf(dudette, dude));
					});
				});
			});
		});
	}
	
	@Test
	public void testFindByNameAndSurname() {
		characterRepo.findByNameAndSurname("Ned", "Stark").ifPresent(nedStark -> {
			if (DO_SYSOUT)
				System.out.println(String.format("## These are the children of %s:", nedStark));
			Collection<HumanBeing> chars = nedStark.getChildren();
			if (DO_SYSOUT)
				chars.forEach(System.out::println);
			assertEquals(2, chars.size());
		});
	}
	
	@Test
	public void testFindChildrenAndGrandchildren() {
		characterRepo.findByNameAndSurname("Catelyn", "Stark").ifPresent(catelynStark -> {
			if (DO_SYSOUT)
				System.out.println(String.format("## These are the children (& grand-children) of %s:", catelynStark));
			Collection<HumanBeing> chars = characterRepo.getAllChildrenAndGrandchildren(catelynStark.getId(), ChildOf.class);
			if (DO_SYSOUT)
				chars.forEach(System.out::println);
			assertEquals(3, chars.size());
			boolean grandChildFound = false;
			for (HumanBeing character : chars) {
				if ("Dude".equals(character.getName()) && "Stark".equals(character.getSurname()))
					grandChildFound = true;
			}
			assertTrue(grandChildFound);
		});
	}

	@Test
	public void testFindChildrenGrandchildrenAndGrandgrandchildren() {
		characterRepo.findByNameAndSurname("Ned", "Stark").ifPresent(nedStark -> {
			if (DO_SYSOUT)
				System.out.println(String.format("## These are the children, grand-children & grand-grand-children of %s:", nedStark));
			Collection<HumanBeing> chars = characterRepo.getAllChildrenMultilevel(nedStark.getId(), (byte)3, ChildOf.class);
			if (DO_SYSOUT)
				chars.forEach(System.out::println);
			assertEquals(4, chars.size());
			boolean grandChildFound = false, grandGrandChildFound = false;
			for (HumanBeing character : chars) {
				if ("Dude".equals(character.getName()) && "Stark".equals(character.getSurname()))
					grandChildFound = true;
				if ("Dudette".equals(character.getName()) && "Stark".equals(character.getSurname()) && 2 == character.getAge() && character.isAlive())
					grandGrandChildFound = true;
			}
			assertTrue(grandChildFound);
			assertTrue(grandGrandChildFound);
		});
	}
	
	@Test
	public void testFindParentsGrandparentsAndGrandgrandparents() {
		characterRepo.findByNameAndSurname("Dudette", "Stark").ifPresent(dudetteStark -> {
			if (DO_SYSOUT)
				System.out.println(String.format("## These are the parents, grand-parents & grand-grand-parents of %s:", dudetteStark));
			Collection<HumanBeing> chars = characterRepo.getAllParentsMultilevel(dudetteStark.getId(), (byte)3, ChildOf.class);
			if (DO_SYSOUT)
				chars.forEach(System.out::println);
			assertEquals(7, chars.size());
			boolean parentFound = false, grandParentFound = false, grandGrandParentFound = false;
			for (HumanBeing character : chars) {
				if ("Dude".equals(character.getName()) && "Stark".equals(character.getSurname()))
					parentFound = true;
				if ("Robb".equals(character.getName()) && "Stark".equals(character.getSurname()))
					grandParentFound = true;
				if ("Jaimie".equals(character.getName()) && "Lanister".equals(character.getSurname()) && 36 == character.getAge() && character.isAlive())
					grandGrandParentFound = true;
			}
			assertTrue(parentFound);
			assertTrue(grandParentFound);
			assertTrue(grandGrandParentFound);
		});
	}
}
