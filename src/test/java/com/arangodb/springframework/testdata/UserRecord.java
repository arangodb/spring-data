package com.arangodb.springframework.testdata;

import com.arangodb.springframework.annotation.ArangoId;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Rev;
import org.springframework.data.annotation.Id;

@Document
public record UserRecord(
        @Id
        String key,

        @ArangoId
        String id,

        @Rev
        String rev,

        String name,

        int age
) {
    public UserRecord(String name, int age) {
        this(null, null, null, name, age);
    }
}
