package com.arangodb.springframework.core.repository.query.derived;

/**
 * Created by F625633 on 24/07/2017.
 */
public class Disjunction {

    private final String array;
    private final String predicate;

    public Disjunction(String array, String predicate) {
        this.array = array;
        this.predicate = predicate;
    }

    public String getArray() {
        return array;
    }

    public String getPredicate() {
        return predicate;
    }
}
