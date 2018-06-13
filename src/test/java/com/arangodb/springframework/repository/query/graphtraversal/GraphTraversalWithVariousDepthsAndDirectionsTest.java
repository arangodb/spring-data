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
 * Tests in-bound, & out-bound graph traversals with various depths for the following graph (level/depth shown in parentheses):
 *   Sansa(1)
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
/*
 * These tests were inspired by spring-data-demo, with additions of extra traversals of various depths, 
 * as well as with additions of OUTBOUND traversals, and other minor modifications, plus conversion to JUnit with assertions of expected results.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ArangoTestConfiguration.class })
public class GraphTraversalWithVariousDepthsAndDirectionsTest extends AbstractArangoTest {
	protected static final Class<?>[] COLLECTIONS = new Class<?>[] { HumanBeing.class, ChildOf.class };
	private static final boolean DO_SYSOUT = false;
	
	@Autowired
	private HumanBeingRepository humanBeingRepo;
	@Autowired
	protected ArangoOperations template;

	public static Collection<HumanBeing> makeCharacters() {
		return Arrays.asList(new HumanBeing("Ned", "Stark", false, 61), new HumanBeing("Catelyn", "Stark", false, 60),
				new HumanBeing("Sansa", "Stark", true, 23), new HumanBeing("Robb", "Stark", false, 40),
				new HumanBeing("Jon", "Snow", true, 56), new HumanBeing("Jaimie", "Lanister", true, 56),
				new HumanBeing("Emily", "Snow", true, 36), new HumanBeing("Dude", "Stark", true, 20), 
				new HumanBeing("Dudette", "Stark", true, 2)
		);
	}

	@Before
	public void setUp() throws Exception {
		for (final Class<?> collection : COLLECTIONS)
			template.collection(collection).drop();
		populateData();
	}

	private void populateData() {
		humanBeingRepo.saveAll(makeCharacters());
		
		humanBeingRepo.findByNameAndSurname("Ned", "Stark").ifPresent(ned -> { // Requires Administrate permission on _system
			humanBeingRepo.findByNameAndSurname("Catelyn", "Stark").ifPresent(catelyn -> {
				humanBeingRepo.findByNameAndSurname("Robb", "Stark").ifPresent(robb -> {
					template.insert(Arrays.asList(new ChildOf(robb, ned), new ChildOf(robb, catelyn)), ChildOf.class);
				});
				humanBeingRepo.findByNameAndSurname("Sansa", "Stark").ifPresent(sansa -> {
					template.insert(Arrays.asList(new ChildOf(sansa, ned), new ChildOf(sansa, catelyn)), ChildOf.class);
				});
			});
		});
		humanBeingRepo.findByNameAndSurname("Jon", "Snow").ifPresent(jon -> {
			humanBeingRepo.findByNameAndSurname("Jaimie", "Lanister").ifPresent(jaimie -> {
				humanBeingRepo.findByNameAndSurname("Emily", "Snow").ifPresent(emily -> {
					template.insert(Arrays.asList(new ChildOf(emily, jon), new ChildOf(emily, jaimie)), ChildOf.class);
				});
			});
		});

		humanBeingRepo.findByNameAndSurname("Robb", "Stark").ifPresent(robb -> {
			humanBeingRepo.findByNameAndSurname("Emily", "Snow").ifPresent(emily-> {
				humanBeingRepo.findByNameAndSurname("Dude", "Stark").ifPresent(dude-> {
					template.insert(Arrays.asList(new ChildOf(dude, robb), new ChildOf(dude, emily)), ChildOf.class);
					humanBeingRepo.findByNameAndSurname("Dudette", "Stark").ifPresent(dudette-> {
						template.insert(new ChildOf(dudette, dude));
					});
				});
			});
		});
	}
	
	@Test
	public void testFindByNameAndSurname() {
		humanBeingRepo.findByNameAndSurname("Ned", "Stark").ifPresent(nedStark -> {
			if (DO_SYSOUT)
				System.out.println(String.format("## These are the children of %s:", nedStark));
			Collection<HumanBeing> kids = nedStark.getChildren();
			if (DO_SYSOUT)
				kids.forEach(System.out::println);
			assertEquals(2, kids.size());
			boolean robbFound = false, sansaFound = false;
			for (HumanBeing human : kids) {
				if ("Robb".equals(human.getName()) && "Stark".equals(human.getSurname()) && 40 == human.getAge() && !human.isAlive())
					robbFound = true;
				if ("Sansa".equals(human.getName()) && "Stark".equals(human.getSurname()) && 23 == human.getAge() && human.isAlive())
					sansaFound = true;
			}
			assertTrue(robbFound && sansaFound);
		});
	}
	
	@Test
	public void testFindChildrenAndGrandchildren() {
		humanBeingRepo.findByNameAndSurname("Catelyn", "Stark").ifPresent(catelynStark -> {
			if (DO_SYSOUT)
				System.out.println(String.format("## These are the children (& grand-children) of %s:", catelynStark));
			Collection<HumanBeing> ancestors = humanBeingRepo.getAllChildrenAndGrandchildren(catelynStark.getId(), ChildOf.class);
			if (DO_SYSOUT)
				ancestors.forEach(System.out::println);
			assertEquals(3, ancestors.size());
			boolean grandChildFound = false;
			for (HumanBeing human : ancestors) {
				if ("Dude".equals(human.getName()) && "Stark".equals(human.getSurname()) && 20 == human.getAge() && human.isAlive())
					grandChildFound = true;
			}
			assertTrue(grandChildFound);
		});
	}

	@Test
	public void testFindChildrenGrandchildrenAndGrandgrandchildren() {
		humanBeingRepo.findByNameAndSurname("Ned", "Stark").ifPresent(nedStark -> {
			if (DO_SYSOUT)
				System.out.println(String.format("## These are the children, grand-children & grand-grand-children of %s:", nedStark));
			Collection<HumanBeing> ancestors = humanBeingRepo.getAllChildrenMultilevel(nedStark.getId(), (byte)3, ChildOf.class);
			if (DO_SYSOUT)
				ancestors.forEach(System.out::println);
			assertEquals(4, ancestors.size());
			boolean grandChildFound = false, grandGrandChildFound = false;
			for (HumanBeing human : ancestors) {
				if ("Dude".equals(human.getName()) && "Stark".equals(human.getSurname()))
					grandChildFound = true;
				if ("Dudette".equals(human.getName()) && "Stark".equals(human.getSurname()) && 2 == human.getAge() && human.isAlive())
					grandGrandChildFound = true;
			}
			assertTrue(grandChildFound);
			assertTrue(grandGrandChildFound);
		});
	}
	
	@Test
	public void testFindParentsGrandparentsAndGrandgrandparents() {
		humanBeingRepo.findByNameAndSurname("Dudette", "Stark").ifPresent(dudetteStark -> {
			if (DO_SYSOUT)
				System.out.println(String.format("## These are the parents, grand-parents & grand-grand-parents of %s:", dudetteStark));
			Collection<HumanBeing> predecessors = humanBeingRepo.getAllParentsMultilevel(dudetteStark.getId(), (byte)3, ChildOf.class);
			if (DO_SYSOUT)
				predecessors.forEach(System.out::println);
			assertEquals(7, predecessors.size());
			boolean parentFound = false, grandParentFound = false, grandGrandParentFound = false;
			for (HumanBeing human : predecessors) {
				if ("Dude".equals(human.getName()) && "Stark".equals(human.getSurname()))
					parentFound = true;
				if ("Robb".equals(human.getName()) && "Stark".equals(human.getSurname()))
					grandParentFound = true;
				if ("Jaimie".equals(human.getName()) && "Lanister".equals(human.getSurname()) && 56 == human.getAge() && human.isAlive())
					grandGrandParentFound = true;
			}
			assertTrue(parentFound);
			assertTrue(grandParentFound);
			assertTrue(grandGrandParentFound);
		});
	}
}
