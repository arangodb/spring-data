package com.arangodb.springframework.core.repository.query.derived;

/**
 * Created by F625633 on 24/07/2017.
 */
public class PartInformation {

    private final boolean isArray;
    private final String clause;

    public PartInformation(boolean isArray, String clause) {
        this.isArray = isArray;
        this.clause = clause;
    }

    public boolean isArray() {
        return isArray;
    }

    public String getClause() {
        return clause;
    }
}
