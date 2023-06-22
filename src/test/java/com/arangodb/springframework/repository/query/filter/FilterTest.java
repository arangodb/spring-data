package com.arangodb.springframework.repository.query.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.arangodb.springframework.repository.query.filter.*;
import org.junit.Test;

public class FilterTest {
    final static String expected = "FILTER (`c`.`a` == \"A\" AND `c`.`b` == \"B\") OR (`c`.`c` == \"C\" AND `c`.`d` == \"D\")";

    @Test
    public void createFilterTest() {
        final Filterable f1 = new CompareExpression(Comparator.EQ, Field.of("c", "a"), "A");
        final Filterable f2 = new CompareExpression(Comparator.EQ, Field.of("c", "b"), "B");
        final Filterable f3 = new CompareExpression(Comparator.EQ, Field.of("c", "c"), "C");
        final Filterable f4 = new CompareExpression(Comparator.EQ, Field.of("c", "d"), "D");

        final Filterable f5 = new GroupExpression(Operator.AND, f1, f2);
        final Filterable f6 = new GroupExpression(Operator.AND, f3, f4);
        final Filterable f7 = new CombinedExpression(Operator.OR, f5, f6);

        final AqlFilterBuilder fb = AqlFilterBuilder.of(f7);

        assertEquals("Filter expressions don't match", expected, fb.toFilterStatement());
    }

    @Test
    public void createFilterOneLineTest() {
        final Filterable f = Field.of("c", "a").eq("A").and(Field.of("c", "b").eq("B")).group()
                .or(Field.of("c", "c").eq("C").and(Field.of("c", "d").eq("D")).group());

        final AqlFilterBuilder fb = AqlFilterBuilder.of(f);

        assertEquals("Filter expressions don't match", expected, fb.toFilterStatement());
    }

    @Test
    public void createFilterMultipleScenariosTest() {
        TestCase[] tests = new TestCase[] {
            TestCase.of("FILTER `c`.`a` == \"A\"", Field.of("c", "a").eq("A")),
            TestCase.of("FILTER `c`.`a` == \"A\" AND `c`.`b` == \"B\"",
                    Field.of("c", "a").eq("A").and(Field.of("c", "b").eq("B"))),
            TestCase.of("FILTER (`c`.`a` == \"A\")", Field.of("c", "a").eq("A").group()),
            TestCase.of("FILTER (`c`.`a` == \"A\") AND (`c`.`b` == \"B\")",
                    Field.of("c", "a").eq("A").group().and(Field.of("c", "b").eq("B").group()))
        };


        AqlFilterBuilder fb;

        for(TestCase test: tests) {
            fb = AqlFilterBuilder.of(test.filter);
            assertEquals("Filter expressions don't match", test.expected, fb.toFilterStatement());
        }

    }

    static class TestCase {
        final String expected;
        final Filterable filter;

        private TestCase(String expected, Filterable filter) {
            this.expected = expected;
            this.filter = filter;
        }

        public static TestCase of(String expected, Filterable filter) {
            return new TestCase(expected, filter);
        }
    }


}
