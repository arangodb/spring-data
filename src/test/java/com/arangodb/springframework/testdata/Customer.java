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

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Ref;

/**
 * @author Mark Vollmary
 *
 */
@Document
public class Customer {

	private String name;
	private String surname;
	private int age;
	private Address address;
	@Ref
	private ShoppingCart shoppingCart;

	public Customer() {
		super();
	}

	public Customer(final String name, final String surname, final int age) {
		super();
		this.name = name;
		this.surname = surname;
		this.age = age;
	}

	public Customer(final String name, final String surname, final int age, final Address address) {
		super();
		this.name = name;
		this.surname = surname;
		this.age = age;
		this.address = address;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(final String surname) {
		this.surname = surname;
	}

	public int getAge() {
		return age;
	}

	public void setAge(final int age) {
		this.age = age;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(final Address address) {
		this.address = address;
	}

	public ShoppingCart getShoppingCart() {
		return shoppingCart;
	}

	public void setShoppingCart(final ShoppingCart shoppingCart) {
		this.shoppingCart = shoppingCart;
	}

}
