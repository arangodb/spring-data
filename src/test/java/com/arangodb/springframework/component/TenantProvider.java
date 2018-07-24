package com.arangodb.springframework.component;

import org.springframework.stereotype.Component;

@Component
public class TenantProvider {

	private final ThreadLocal<String> id;

	public TenantProvider() {
		super();
		id = new ThreadLocal<>();
	}

	public String getId() {
		return id.get();
	}

	public void setId(final String id) {
		this.id.set(id);
	}

}