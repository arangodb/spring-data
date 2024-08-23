package com.arangodb.springframework.repository;

import com.arangodb.springframework.annotation.Document;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@Document
public class IntegerIdTestEntity implements IdTestEntity<Integer> {
    @Id
    private Integer id;
}
