package com.arangodb.springframework.testdata;

public class Nickname {
    private String value;

    public Nickname() {
    }

    public Nickname(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Nickname{" +
                "value='" + value + '\'' +
                '}';
    }
}
