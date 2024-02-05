package com.arangodb.springframework.repository;

import com.arangodb.springframework.testdata.UserImmutable;

public interface UserImmutableRepository extends ArangoRepository<UserImmutable, String> {
}

