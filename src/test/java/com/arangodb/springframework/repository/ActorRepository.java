package com.arangodb.springframework.repository;

import com.arangodb.springframework.testdata.Actor;

public interface ActorRepository extends ArangoRepository<Actor, String> {
}
