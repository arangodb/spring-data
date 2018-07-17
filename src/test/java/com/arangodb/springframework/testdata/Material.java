package com.arangodb.springframework.testdata;

import org.springframework.data.annotation.Id;

import com.arangodb.springframework.annotation.Key;
import com.arangodb.springframework.annotation.Rev;

/**
 * Created by markmccormick on 24/08/2017.
 */
@SuppressWarnings("deprecation")
public class Material {

	@Id
	private String id;
	@Key
	private String key;
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
}
