package com.arangodb.springframework.testdata;

import com.arangodb.springframework.annotation.Field;

public class CustomerNameProjection {

	@Field("customer-name")
	private String name;

	public String getName() {
		return name;
	}

}
