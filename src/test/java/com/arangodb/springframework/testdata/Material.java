package com.arangodb.springframework.testdata;

import org.springframework.data.annotation.Id;

import com.arangodb.springframework.annotation.Rev;

import java.util.Objects;

/**
 * Created by markmccormick on 24/08/2017.
 */
public class Material {

	@Id
	private String id;
	@Rev
	private String rev;
	private String name;

	public Material(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Material material = (Material) o;
		return Objects.equals(id, material.id) && Objects.equals(rev, material.rev) && Objects.equals(name, material.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, rev, name);
	}
}
