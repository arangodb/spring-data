package com.arangodb.springframework.core.repository.query.derived;

/**
 * Created by F625633 on 24/07/2017.
 */
public class Conjunction {

    private final String array;
    private final String predicate;

    public Conjunction(String array, String predicate) {
        this.array = array;
        this.predicate = predicate;
    }

    public String getArray() {
        return array;
    }

    public String getPredicate() {
        return predicate;
    }

    public boolean isArray() { return !array.isEmpty(); }

    public boolean hasPredicate() { return !predicate.isEmpty(); }

    public boolean isComposite() { return isArray() && hasPredicate(); }
}
