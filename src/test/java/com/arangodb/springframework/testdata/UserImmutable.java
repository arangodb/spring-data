package com.arangodb.springframework.testdata;

import com.arangodb.springframework.annotation.ArangoId;
import com.arangodb.springframework.annotation.Rev;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;

import java.util.Objects;

public final class UserImmutable {
    @Id
    private final String key;

    @ArangoId
    private final String id;

    @Rev
    private final String rev;

    private final String name;

    private final int age;

    @PersistenceCreator
    public UserImmutable(String key, String id, String rev, String name, int age) {
        this.key = key;
        this.id = id;
        this.rev = rev;
        this.name = name;
        this.age = age;
    }

    public UserImmutable(String name, int age) {
        this(null, null, null, name, age);
    }

    public String getKey() {
        return key;
    }

    public String getId() {
        return id;
    }

    public String getRev() {
        return rev;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserImmutable that = (UserImmutable) o;
        return age == that.age && Objects.equals(key, that.key) && Objects.equals(id, that.id) && Objects.equals(rev, that.rev) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, id, rev, name, age);
    }

    @Override
    public String toString() {
        return "ImmutableUser{" +
                "key='" + key + '\'' +
                ", id='" + id + '\'' +
                ", rev='" + rev + '\'' +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
