package com.arangodb.springframework.repository;

import com.arangodb.springframework.testdata.Movie;

public interface MovieRepository extends ArangoRepository<Movie, String> {
}
