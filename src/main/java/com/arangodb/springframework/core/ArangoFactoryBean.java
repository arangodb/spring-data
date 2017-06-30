/*
 * DISCLAIMER
 *
 * Copyright 2017 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.springframework.core;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.arangodb.ArangoDB;
import com.arangodb.internal.ArangoDBConstants;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoFactoryBean extends AbstractFactoryBean<ArangoDB.Builder> {

	public static final String PROPERTY_NAME_HOST = "host";
	public static final String PROPERTY_NAME_PORT = "port";
	public static final String PROPERTY_NAME_USERNAME = "username";
	public static final String PROPERTY_NAME_PASSWORD = "password";

	private String host;
	private Integer port;
	private String username;
	private String password;

	public ArangoFactoryBean() {
		super();
	}

	public String getHost() {
		return host;
	}

	public void setHost(final String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(final Integer port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	@Override
	public Class<?> getObjectType() {
		return ArangoDB.Builder.class;
	}

	@Override
	protected ArangoDB.Builder createInstance() throws Exception {
		final ArangoDB.Builder builder = new ArangoDB.Builder();
		if (username != null) {
			builder.user(username).password(password);
		}
		if (host != null) {
			builder.host(host, port != null ? port : ArangoDBConstants.DEFAULT_PORT);
		}
		return builder;
	}

}
