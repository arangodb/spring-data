package com.arangodb.springframework.repository;

import com.arangodb.springframework.annotation.Document;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.concurrent.ThreadLocalRandom;


@Data
@Document
public class LongIdTestEntity implements IdTestEntity<Long> {

    public static LongIdTestEntity create() {
        LongIdTestEntity res = new LongIdTestEntity();
        res.setValue(ThreadLocalRandom.current().nextLong());
        return res;
    }

    @Id
    private Long id;
    private Long value;
}
