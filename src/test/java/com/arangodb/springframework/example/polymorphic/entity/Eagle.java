/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
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

package com.arangodb.springframework.example.polymorphic.entity;

import org.springframework.data.annotation.Id;

import java.util.Objects;

/**
 * @author Michele Rastelli
 */
public class Eagle implements Animal {

	@Id
	private String id;

	private String name;

	private Double wingspan;

	public Eagle() {
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getWingspan() {
		return wingspan;
	}

	public void setWingspan(Double wingspan) {
		this.wingspan = wingspan;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Eagle eagle = (Eagle) o;
		return Objects.equals(id, eagle.id) && Objects.equals(name, eagle.name) && Objects
				.equals(wingspan, eagle.wingspan);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, wingspan);
	}
}
