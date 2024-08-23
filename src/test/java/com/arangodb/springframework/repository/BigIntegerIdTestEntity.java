package com.arangodb.springframework.repository;

import com.arangodb.springframework.annotation.Document;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.math.BigInteger;


@Data
@Document
public class BigIntegerIdTestEntity implements IdTestEntity<BigInteger> {
    @Id
    private BigInteger id;
}
