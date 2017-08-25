package com.arangodb.springframework.testdata;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Key;
import com.arangodb.springframework.annotation.Rev;
import org.springframework.data.annotation.Id;

import java.util.List;

@Document("customer")
public class IncompleteCustomer {
    @Id
    private String id;
    @Key
    private String key;
    @Rev
    private String rev;

    private String name;
    private List<String> stringList;

    public IncompleteCustomer(String name, List<String> stringList) {
        this.name = name;
        this.stringList = stringList;
    }
}
