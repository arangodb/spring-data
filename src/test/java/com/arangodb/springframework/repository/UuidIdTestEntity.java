package com.arangodb.springframework.repository;

import com.arangodb.springframework.annotation.Document;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Data
@Document
public class UuidIdTestEntity implements IdTestEntity<UUID> {

    public static UuidIdTestEntity create() {
        UuidIdTestEntity res = new UuidIdTestEntity();
        res.setId(UUID.randomUUID());
        res.setValue(UUID.randomUUID());
        return res;
    }

    @Id
    private UUID id;
    private UUID value;
}
