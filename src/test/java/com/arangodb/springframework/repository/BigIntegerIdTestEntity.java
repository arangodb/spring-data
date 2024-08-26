package com.arangodb.springframework.repository;

import com.arangodb.springframework.annotation.Document;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;


@Data
@Document
public class BigIntegerIdTestEntity implements IdTestEntity<BigInteger> {

    public static BigIntegerIdTestEntity create() {
        BigIntegerIdTestEntity res = new BigIntegerIdTestEntity();
        res.setValue(BigInteger.valueOf(ThreadLocalRandom.current().nextLong()));
        return res;
    }

    @Id
    private BigInteger id;
    private BigInteger value;
}
