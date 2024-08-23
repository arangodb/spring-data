package com.arangodb.springframework.repository;

import com.arangodb.springframework.annotation.Document;
import lombok.Data;
import org.springframework.data.annotation.Id;


@Data
@Document
public class LongIdTestEntity implements IdTestEntity<Long> {
    @Id
    private Long id;
}
