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

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 
 * @author Christian Lechner
 */
public class AqlUtilsTest {

	@Test
	public void buildLimitClauseTest() {
		assertThat(AqlUtils.buildLimitClause(Pageable.unpaged()), is(""));
		assertThat(AqlUtils.buildLimitClause(PageRequest.of(0, 1)), is("LIMIT 0, 1"));
		assertThat(AqlUtils.buildLimitClause(PageRequest.of(10, 20)), is("LIMIT 200, 20"));
	}

	@Test
	public void buildPageableClauseTest() {
		// Special cases
		assertThat(AqlUtils.buildPageableClause(Pageable.unpaged()), is(""));

		// Paging without sort
		assertThat(AqlUtils.buildPageableClause(PageRequest.of(0, 1)), is("LIMIT 0, 1"));
		assertThat(AqlUtils.buildPageableClause(PageRequest.of(5, 10)), is("LIMIT 50, 10"));

		// Paging with sort
		assertThat(AqlUtils.buildPageableClause(PageRequest.of(2, 10, Direction.ASC, "property")),
			is("SORT `property` ASC LIMIT 20, 10"));
		assertThat(AqlUtils.buildPageableClause(PageRequest.of(2, 10, Direction.ASC, "property"), "var"),
			is("SORT `var`.`property` ASC LIMIT 20, 10"));

		assertThat(AqlUtils.buildPageableClause(PageRequest.of(2, 10, Direction.DESC, "property", "property2")),
			is("SORT `property` DESC, `property2` DESC LIMIT 20, 10"));
		assertThat(AqlUtils.buildPageableClause(PageRequest.of(2, 10, Direction.DESC, "property", "property2"), "var"),
			is("SORT `var`.`property` DESC, `var`.`property2` DESC LIMIT 20, 10"));

		assertThat(
			AqlUtils.buildPageableClause(
				PageRequest.of(2, 10, Sort.by("ascProp").and(Sort.by(Direction.DESC, "descProp")))),
			is("SORT `ascProp` ASC, `descProp` DESC LIMIT 20, 10"));
		assertThat(
			AqlUtils.buildPageableClause(
				PageRequest.of(2, 10, Sort.by("ascProp").and(Sort.by(Direction.DESC, "descProp"))), "var"),
			is("SORT `var`.`ascProp` ASC, `var`.`descProp` DESC LIMIT 20, 10"));
	}

	@Test
	public void buildSortClauseTest() {
		// Special cases
		assertThat(AqlUtils.buildSortClause(Sort.unsorted()), is(""));

		// Others
		assertThat(AqlUtils.buildSortClause(Sort.by("property")), is("SORT `property` ASC"));
		assertThat(AqlUtils.buildSortClause(Sort.by("property"), "var"), is("SORT `var`.`property` ASC"));

		assertThat(AqlUtils.buildSortClause(Sort.by(Direction.DESC, "property")), is("SORT `property` DESC"));
		assertThat(AqlUtils.buildSortClause(Sort.by(Direction.DESC, "property"), "var"),
			is("SORT `var`.`property` DESC"));

		assertThat(AqlUtils.buildSortClause(Sort.by(Direction.DESC, "property", "property2")),
			is("SORT `property` DESC, `property2` DESC"));
		assertThat(AqlUtils.buildSortClause(Sort.by(Direction.DESC, "property", "property2"), "var"),
			is("SORT `var`.`property` DESC, `var`.`property2` DESC"));

		assertThat(AqlUtils.buildSortClause(Sort.by(Direction.DESC, "property").and(Sort.by("property2"))),
			is("SORT `property` DESC, `property2` ASC"));
		assertThat(AqlUtils.buildSortClause(Sort.by(Direction.DESC, "property").and(Sort.by("property2")), "var"),
			is("SORT `var`.`property` DESC, `var`.`property2` ASC"));
	}

	@Test
	public void sortClauseEscapingTest() {
		assertThat(AqlUtils.buildSortClause(Sort.by("property")), is("SORT `property` ASC"));

		assertThat(AqlUtils.buildSortClause(Sort.by("`property`")), is("SORT `property` ASC"));

		assertThat(AqlUtils.buildSortClause(Sort.by("`pro\\`perty\\``")), is("SORT `pro\\`perty\\`` ASC"));

		assertThat(AqlUtils.buildSortClause(Sort.by("`dont.split.property`")), is("SORT `dont.split.property` ASC"));

		assertThat(AqlUtils.buildSortClause(Sort.by("property.`property`")), is("SORT `property`.`property` ASC"));

		assertThat(AqlUtils.buildSortClause(Sort.by("property.`.`.`property`")),
			is("SORT `property`.`.`.`property` ASC"));

		assertThat(AqlUtils.buildSortClause(Sort.by("property.\\.property")),
			is("SORT `property`.`\\\\`.`property` ASC"));

		assertThat(AqlUtils.buildSortClause(Sort.by("property.\\\\`.property")),
			is("SORT `property`.`\\\\\\``.`property` ASC"));

		assertThat(AqlUtils.buildSortClause(Sort.by("`property.\\`.property`")),
				is("SORT `property.\\`.property` ASC"));

		assertThat(AqlUtils.buildSortClause(Sort.by("`property.\\``.property")),
				is("SORT `property.\\``.`property` ASC"));

		assertThat(AqlUtils.buildSortClause(Sort.by("`property..property`")), is("SORT `property..property` ASC"));

		assertThat(AqlUtils.buildSortClause(Sort.by("property\\. REMOVE doc IN collection //")),
				is("SORT `property\\\\`.` REMOVE doc IN collection //` ASC"));

		assertThat(AqlUtils.buildSortClause(Sort.by("values[0].date")),
				is("SORT `values`[0].`date` ASC"));

		assertThat(AqlUtils.buildSortClause(Sort.by("`values[0]`.date")),
				is("SORT `values[0]`.`date` ASC"));

		// Illegal sort properties

		assertThrows(IllegalArgumentException.class,
				() -> AqlUtils.buildSortClause(Sort.by(".property")));

		assertThrows(IllegalArgumentException.class,
				() -> AqlUtils.buildSortClause(Sort.by("property.")));

		assertThrows(IllegalArgumentException.class,
				() -> AqlUtils.buildSortClause(Sort.by("property..property")));

		assertThrows(IllegalArgumentException.class,
				() -> AqlUtils.buildSortClause(Sort.by("property.`property")));

		assertThrows(IllegalArgumentException.class,
				() -> AqlUtils.buildSortClause(Sort.by("pro`perty.property")));

		assertThrows(IllegalArgumentException.class,
				() -> AqlUtils.buildSortClause(Sort.by("`property``.property")));

		assertThrows(IllegalArgumentException.class,
				() -> AqlUtils.buildSortClause(Sort.by("`property```.property")));

		assertThrows(IllegalArgumentException.class,
				() -> AqlUtils.buildSortClause(Sort.by("`property.`\\`.property`")));

		assertThrows(IllegalArgumentException.class,
				() -> AqlUtils.buildSortClause(Sort.by("`property.`\\``.property`")));

		assertThrows(IllegalArgumentException.class,
				() -> AqlUtils.buildSortClause(Sort.by("`property`.\\``.property`")));

	}

}
