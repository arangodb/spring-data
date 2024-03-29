package com.arangodb.springframework.repository.query.graphtraversal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
// Avoiding this since Eclipse draws unnecessary attention to it due to is(java.lang.Class<T> type) being deprecated:
//import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.arangodb.springframework.AbstractArangoTest;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.repository.HumanBeingRepository;
import com.arangodb.springframework.testdata.ChildOf;
import com.arangodb.springframework.testdata.HumanBeing;

/**
 * Tests in-bound, & out-bound graph traversals with various depths for the following graph (level/depth shown in
 * parentheses): Sansa(1) / \ Ned(0) Catelyn(0) Jon(0) Jaimie(0) \ / \ / Robb(1) Emily(1) \ / \ / \ / \ / Dude(2) |
 * Dudette(3)
 * 
 * @author Reşat SABIQ
 */
/*
 * These tests were inspired by spring-data-demo, with additions of extra traversals of various depths, as well as with
 * additions of OUTBOUND traversals, and other minor modifications, plus conversion to JUnit with assertions of expected
 * results.
 */
public class GraphTraversalWithVariousDepthsAndDirectionsTest extends AbstractArangoTest {

	@Autowired
	private HumanBeingRepository humanBeingRepo;
	@Autowired
	protected ArangoOperations template;

	// For easier & more maintainable comparisons in asserts:
	HumanBeing ned = new HumanBeing("Ned", "Stark", false, 61);
	HumanBeing catelyn = new HumanBeing("Catelyn", "Stark", false, 60);
	HumanBeing sansa = new HumanBeing("Sansa", "Stark", true, 23);
	HumanBeing robb = new HumanBeing("Robb", "Stark", false, 40);
	HumanBeing jon = new HumanBeing("Jon", "Snow", true, 56);
	HumanBeing jaimie = new HumanBeing("Jaimie", "Lanister", true, 56);
	HumanBeing emily = new HumanBeing("Emily", "Snow", true, 36);
	HumanBeing dude = new HumanBeing("Dude", "Stark", true, 20);
	HumanBeing dudette = new HumanBeing("Dudette", "Stark", true, 2);

	public GraphTraversalWithVariousDepthsAndDirectionsTest() {
		super(HumanBeing.class, ChildOf.class);
	}

	public Collection<HumanBeing> obtainHumanBeingsCollection() {
		return Arrays.asList(ned, catelyn, sansa, robb, jon, jaimie, emily, dude, dudette);
	}

	@BeforeEach
	public void setUp() {
		populateData();
	}

	private Optional<HumanBeing> findByExample(final HumanBeing example) {
		return humanBeingRepo.findByNameAndSurname(example.getName(), example.getSurname());
	}

	private void populateData() {
		humanBeingRepo.saveAll(obtainHumanBeingsCollection());

		findByExample(ned).ifPresent(nedStark -> { // Requires Administrate permission on _system
			findByExample(catelyn).ifPresent(catelynStark -> {
				findByExample(robb).ifPresent(robbStark -> {
					template.insertAll(
						Arrays.asList(new ChildOf(robbStark, nedStark), new ChildOf(robbStark, catelynStark)),
						ChildOf.class);
				});
				findByExample(sansa).ifPresent(sansaStark -> {
					template.insertAll(
						Arrays.asList(new ChildOf(sansaStark, nedStark), new ChildOf(sansaStark, catelynStark)),
						ChildOf.class);
				});
			});
		});
		findByExample(jon).ifPresent(jonSnow -> {
			findByExample(jaimie).ifPresent(jaimieLanister -> {
				findByExample(emily).ifPresent(emilySnow -> {
					template.insertAll(
						Arrays.asList(new ChildOf(emilySnow, jonSnow), new ChildOf(emilySnow, jaimieLanister)),
						ChildOf.class);
				});
			});
		});

		findByExample(robb).ifPresent(robbStark -> {
			findByExample(emily).ifPresent(emilySnow -> {
				findByExample(dude).ifPresent(dudeStark -> {
					template.insertAll(Arrays.asList(new ChildOf(dudeStark, robbStark), new ChildOf(dudeStark, emilySnow)),
						ChildOf.class);
					findByExample(dudette).ifPresent(dudetteStark -> {
						template.insert(new ChildOf(dudetteStark, dudeStark));
					});
				});
			});
		});
	}

	@Test
	public void testFindByNameAndSurname() {
		findByExample(ned).ifPresent(nedStark -> {
			final Collection<HumanBeing> kids = nedStark.getChildren();
			assertThat(kids.size(), equalTo(2));
			for (final HumanBeing human : kids) {
				assertThat(human, anyOf(Matchers.is(robb), Matchers.is(sansa)));
			}
		});
	}

	@Test
	public void testFindChildrenAndGrandchildren() {
		findByExample(catelyn).ifPresent(catelynStark -> {
			final Collection<HumanBeing> ancestors = humanBeingRepo
					.getAllChildrenAndGrandchildren("humanBeing/" + catelynStark.getId(), ChildOf.class);
			assertThat(ancestors.size(), equalTo(3));
			for (final HumanBeing human : ancestors) {
				assertThat(human, anyOf(Matchers.is(robb), Matchers.is(sansa), Matchers.is(dude)));
			}
		});
	}

	@Test
	public void testFindChildrenGrandchildrenAndGrandgrandchildren() {
		findByExample(ned).ifPresent(nedStark -> {
			final Collection<HumanBeing> ancestors = humanBeingRepo
					.getAllChildrenMultilevel("humanBeing/" + nedStark.getId(), (byte) 3, ChildOf.class);
			assertThat(ancestors.size(), equalTo(4));
			for (final HumanBeing human : ancestors) {
				assertThat(human,
					anyOf(Matchers.is(robb), Matchers.is(sansa), Matchers.is(dude), Matchers.is(dudette)));
				assertThat(human, not(emily));
			}
		});
	}

	@Test
	public void testFindParentsGrandparentsAndGrandgrandparents() {
		findByExample(dudette).ifPresent(dudetteStark -> {
			final Collection<HumanBeing> predecessors = humanBeingRepo
					.getAllParentsMultilevel("humanBeing/" + dudetteStark.getId(), (byte) 3, ChildOf.class);
			assertThat(predecessors.size(), equalTo(7));
			final List<Matcher<? super HumanBeing>> matchers = Arrays.asList(Matchers.is(dude), Matchers.is(robb),
				Matchers.is(emily), Matchers.is(ned), Matchers.is(catelyn), Matchers.is(jon), Matchers.is(jaimie));
			for (final HumanBeing human : predecessors) {
				assertThat(human, anyOf(matchers));
				assertThat(human, not(sansa));
			}
		});
	}
}
