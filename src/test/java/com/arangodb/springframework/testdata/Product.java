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

package com.arangodb.springframework.testdata;

import com.arangodb.springframework.annotation.*;
import org.springframework.data.annotation.Id;

/**
 * @author Mark Vollmary
 *
 */
@GeoIndex(fields = {"location"})
public class Product {

	@Id
	private String id;
	@Key
	private String key;
	@Rev
	private String rev;
	private String name;
	@Field("description")
	private String desc;
	private double[] location;

	@Relations(edge = Contains.class)
	private Material contains;

	public Product() {
		super();
	}

	public Product(final String name) {
		super();
		this.name = name;
	}

	public Product(final String name, double[] location) {
		this(name);
		this.location = location;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public String getRev() {
		return rev;
	}

	public void setRev(final String rev) {
		this.rev = rev;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(final String desc) {
		this.desc = desc;
	}

	public Material getContains() {
		return contains;
	}

	public void setContains(Material contains) {
		this.contains = contains;
	}

}
