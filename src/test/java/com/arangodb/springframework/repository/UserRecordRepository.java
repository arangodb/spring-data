package com.arangodb.springframework.repository;

import com.arangodb.springframework.testdata.UserRecord;

public interface UserRecordRepository extends ArangoRepository<UserRecord, String> {
}

