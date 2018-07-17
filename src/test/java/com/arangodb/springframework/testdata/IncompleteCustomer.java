package com.arangodb.springframework.testdata;

import java.util.List;

import org.springframework.data.annotation.Id;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Key;
import com.arangodb.springframework.annotation.Rev;

@SuppressWarnings("deprecation")
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

	public IncompleteCustomer(final String name, final List<String> stringList) {
		this.name = name;
		this.stringList = stringList;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public List<String> getStringList() {
		return stringList;
	}

	public void setStringList(final List<String> stringList) {
		this.stringList = stringList;
	}

}
