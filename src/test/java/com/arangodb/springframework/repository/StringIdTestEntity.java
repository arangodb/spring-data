package com.arangodb.springframework.repository;

import com.arangodb.springframework.annotation.Document;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.UUID;


@Data
@Document
public class StringIdTestEntity implements IdTestEntity<String> {

    public static StringIdTestEntity create() {
        StringIdTestEntity res = new StringIdTestEntity();
        res.setValue(UUID.randomUUID().toString());
        return res;
    }

    @Id
    private String id;
    private String value;
}
