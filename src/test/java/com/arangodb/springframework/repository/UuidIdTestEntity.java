package com.arangodb.springframework.repository;

import com.arangodb.springframework.annotation.Document;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Data
@Document
public class UuidIdTestEntity implements IdTestEntity<UUID> {
    @Id
    private UUID id;
}
