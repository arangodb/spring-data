package com.arangodb.springframework.repository.query;

import java.util.Collection;

public class QueryWithCollections {

    private final String query;
    private final Collection<String> collections;

    public QueryWithCollections(String query, Collection<String> collections) {
        this.query = query;
        this.collections = collections;
    }

    public String getQuery() {
        return query;
    }

    public Collection<String> getCollections() {
        return collections;
    }
}
