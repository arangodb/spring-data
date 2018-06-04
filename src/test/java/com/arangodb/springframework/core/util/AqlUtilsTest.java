/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.springframework.core.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * 
 * @author Christian Lechner
 */
public class AqlUtilsTest {

	@Test
	public void buildLimitClauseTest() {
		assertThat(AqlUtils.buildLimitClause(null), is(""));
		assertThat(AqlUtils.buildLimitClause(new PageRequest(0, 1)), is("LIMIT 0, 1"));
		assertThat(AqlUtils.buildLimitClause(new PageRequest(10, 20)), is("LIMIT 200, 20"));
	}

	@Test
	public void buildPageableClauseTest() {
		// Special cases
		assertThat(AqlUtils.buildPageableClause(null), is(""));

		// Paging without sort
		assertThat(AqlUtils.buildPageableClause(new PageRequest(0, 1)), is("LIMIT 0, 1"));
		assertThat(AqlUtils.buildPageableClause(new PageRequest(5, 10)), is("LIMIT 50, 10"));

		// Paging with sort
		assertThat(AqlUtils.buildPageableClause(new PageRequest(2, 10, Direction.ASC, "property")),
			is("SORT `property` ASC LIMIT 20, 10"));
		assertThat(AqlUtils.buildPageableClause(new PageRequest(2, 10, Direction.ASC, "property"), "var"),
			is("SORT `var`.`property` ASC LIMIT 20, 10"));

		assertThat(AqlUtils.buildPageableClause(new PageRequest(2, 10, Direction.DESC, "property", "property2")),
			is("SORT `property` DESC, `property2` DESC LIMIT 20, 10"));
		assertThat(AqlUtils.buildPageableClause(new PageRequest(2, 10, Direction.DESC, "property", "property2"), "var"),
			is("SORT `var`.`property` DESC, `var`.`property2` DESC LIMIT 20, 10"));

		assertThat(
			AqlUtils.buildPageableClause(
				new PageRequest(2, 10, new Sort("ascProp").and(new Sort(Direction.DESC, "descProp")))),
			is("SORT `ascProp` ASC, `descProp` DESC LIMIT 20, 10"));
		assertThat(
			AqlUtils.buildPageableClause(
				new PageRequest(2, 10, new Sort("ascProp").and(new Sort(Direction.DESC, "descProp"))), "var"),
			is("SORT `var`.`ascProp` ASC, `var`.`descProp` DESC LIMIT 20, 10"));
	}

	@Test
	public void buildSortClauseTest() {
		// Special cases
		assertThat(AqlUtils.buildSortClause(null), is(""));

		// Others
		assertThat(AqlUtils.buildSortClause(new Sort("property")), is("SORT `property` ASC"));
		assertThat(AqlUtils.buildSortClause(new Sort("property"), "var"), is("SORT `var`.`property` ASC"));

		assertThat(AqlUtils.buildSortClause(new Sort(Direction.DESC, "property")), is("SORT `property` DESC"));
		assertThat(AqlUtils.buildSortClause(new Sort(Direction.DESC, "property"), "var"),
			is("SORT `var`.`property` DESC"));

		assertThat(AqlUtils.buildSortClause(new Sort(Direction.DESC, "property", "property2")),
			is("SORT `property` DESC, `property2` DESC"));
		assertThat(AqlUtils.buildSortClause(new Sort(Direction.DESC, "property", "property2"), "var"),
			is("SORT `var`.`property` DESC, `var`.`property2` DESC"));

		assertThat(AqlUtils.buildSortClause(new Sort(Direction.DESC, "property").and(new Sort("property2"))),
			is("SORT `property` DESC, `property2` ASC"));
		assertThat(AqlUtils.buildSortClause(new Sort(Direction.DESC, "property").and(new Sort("property2")), "var"),
			is("SORT `var`.`property` DESC, `var`.`property2` ASC"));
	}

	@Test
	public void sortClauseEscapingTest() {
		assertThat(AqlUtils.buildSortClause(new Sort("property")), is("SORT `property` ASC"));

		assertThat(AqlUtils.buildSortClause(new Sort("`property`")), is("SORT `property` ASC"));

		assertThat(AqlUtils.buildSortClause(new Sort("`pro\\`perty\\``")), is("SORT `pro\\`perty\\`` ASC"));

		assertThat(AqlUtils.buildSortClause(new Sort("`dont.split.property`")), is("SORT `dont.split.property` ASC"));

		assertThat(AqlUtils.buildSortClause(new Sort("property.`property`")), is("SORT `property`.`property` ASC"));

		assertThat(AqlUtils.buildSortClause(new Sort("property.`.`.`property`")),
			is("SORT `property`.`.`.`property` ASC"));

		assertThat(AqlUtils.buildSortClause(new Sort("property.\\.property")),
			is("SORT `property`.`\\\\`.`property` ASC"));

		assertThat(AqlUtils.buildSortClause(new Sort("property.\\\\`.property")),
			is("SORT `property`.`\\\\\\``.`property` ASC"));

		assertThat(AqlUtils.buildSortClause(new Sort("`property.\\`.property`")),
			is("SORT `property.\\`.property` ASC"));

		assertThat(AqlUtils.buildSortClause(new Sort("`property.\\``.property")),
			is("SORT `property.\\``.`property` ASC"));

		assertThat(AqlUtils.buildSortClause(new Sort("`property..property`")), is("SORT `property..property` ASC"));

		assertThat(AqlUtils.buildSortClause(new Sort("property\\. REMOVE doc IN collection //")),
			is("SORT `property\\\\`.` REMOVE doc IN collection //` ASC"));

		// Illegal sort properties

		try {
			AqlUtils.buildSortClause(new Sort(".property"));
			Assert.fail();
		} catch (final IllegalArgumentException e) {
		}

		try {
			AqlUtils.buildSortClause(new Sort("property."));
			Assert.fail();
		} catch (final IllegalArgumentException e) {
		}

		try {
			AqlUtils.buildSortClause(new Sort("property..property"));
			Assert.fail();
		} catch (final IllegalArgumentException e) {
		}

		try {
			AqlUtils.buildSortClause(new Sort("property.`property"));
			Assert.fail();
		} catch (final IllegalArgumentException e) {
		}

		try {
			AqlUtils.buildSortClause(new Sort("pro`perty.property"));
			Assert.fail();
		} catch (final IllegalArgumentException e) {
		}

		try {
			AqlUtils.buildSortClause(new Sort("`property``.property"));
			Assert.fail();
		} catch (final IllegalArgumentException e) {
		}

		try {
			AqlUtils.buildSortClause(new Sort("`property```.property"));
			Assert.fail();
		} catch (final IllegalArgumentException e) {
		}

		try {
			AqlUtils.buildSortClause(new Sort("`property.`\\`.property`"));
			Assert.fail();
		} catch (final IllegalArgumentException e) {
		}

		try {
			AqlUtils.buildSortClause(new Sort("`property.`\\``.property`"));
			Assert.fail();
		} catch (final IllegalArgumentException e) {
		}

		try {
			AqlUtils.buildSortClause(new Sort("`property`.\\``.property`"));
			Assert.fail();
		} catch (final IllegalArgumentException e) {
		}

	}

}
