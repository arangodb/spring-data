package com.arangodb.springframework.testdata;

import com.arangodb.springframework.annotation.Key;
import com.arangodb.springframework.annotation.Rev;
import org.springframework.data.annotation.Id;

/**
 * Created by markmccormick on 24/08/2017.
 */
public class Material {

    @Id
    private String id;
    @Key
    private String key;
    @Rev
    private String rev;
    private String name;

    public Material(String name) {
        this.name = name;
    }
}
