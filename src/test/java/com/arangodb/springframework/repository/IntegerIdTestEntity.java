package com.arangodb.springframework.repository;

import com.arangodb.springframework.annotation.Document;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.concurrent.ThreadLocalRandom;

@Data
@Document
public class IntegerIdTestEntity implements IdTestEntity<Integer> {

    public static IntegerIdTestEntity create() {
        IntegerIdTestEntity res = new IntegerIdTestEntity();
        res.setValue(ThreadLocalRandom.current().nextInt());
        return res;
    }

    @Id
    private Integer id;
    private Integer value;
}
