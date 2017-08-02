package com.arangodb.springframework.core.repository.query.derived;

/**
 * Created by F625633 on 24/07/2017.
 */
public class ConjunctionBuilder {

    private static final String ARRAY_DELIMITER = ", ";
    private static final String PREDICATE_DELIMITER = " AND ";

    private final StringBuilder arrayStringBuilder = new StringBuilder();
    private final StringBuilder predicateStringBuilder = new StringBuilder();

    private int arrays = 0;

    public void add(PartInformation partInformation) {
        if (partInformation.isArray()) {
            ++arrays;
            arrayStringBuilder.append((arrayStringBuilder.length() == 0 ? "" : ARRAY_DELIMITER)
                    + partInformation.getClause());
        } else {
            predicateStringBuilder.append((predicateStringBuilder.length() == 0 ? "" : PREDICATE_DELIMITER)
                    + partInformation.getClause());
        }
    }

    private String buildArrayString() {
        if (arrays > 1) return "INTERSECTION(" + arrayStringBuilder.toString() + ")";
        return arrayStringBuilder.toString();
    }

    public Conjunction build() {
        return new Conjunction(buildArrayString(), predicateStringBuilder.toString());
    }
}
