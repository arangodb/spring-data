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

import java.util.Collection;

import org.springframework.data.annotation.Id;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.GeoIndexed;
import com.arangodb.springframework.annotation.Key;
import com.arangodb.springframework.annotation.Ref;
import com.arangodb.springframework.annotation.Relations;
import com.arangodb.springframework.annotation.Rev;

/**
 * @author Mark Vollmary
 *
 */
@SuppressWarnings("deprecation")
@Document
public class Customer {

	@Id
	private String id;
	@Key
	private String key;
	@Rev
	private String rev;
	private String name;
	private String surname;
	private int age;
	private Address address;

	private boolean alive;
	@GeoIndexed
	private int[] location;
	private Iterable<Integer> integerList;
	private String[] stringArray;
	private Iterable<String> stringList;
	private Customer nestedCustomer;
	private Iterable<Customer> nestedCustomers;

	@Ref
	private ShoppingCart shoppingCart;
	@Relations(edges = { Owns.class })
	private Collection<Product> owns;

	public Customer() {
		super();
	}

	public Customer(final String name, final String surname, final int age) {
		super();
		this.name = name;
		this.surname = surname;
		this.age = age;
	}

	public Customer(final String name, final String surname, final int age, final boolean alive) {
		super();
		this.name = name;
		this.surname = surname;
		this.age = age;
		this.alive = alive;
	}

	public Customer(final String name, final String surname, final int age, final Address address) {
		super();
		this.name = name;
		this.surname = surname;
		this.age = age;
		this.address = address;
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

	public Collection<Product> getOwns() {
		return owns;
	}

	public void setOwns(final Collection<Product> owns) {
		this.owns = owns;
	}

	public boolean isAlive() {
		return alive;
	}

	public void setAlive(final boolean alive) {
		this.alive = alive;
	}

	public int[] getLocation() {
		return location;
	}

	public void setLocation(final int[] location) {
		this.location = location;
	}

	public Iterable<Integer> getIntegerList() {
		return integerList;
	}

	public void setIntegerList(final Iterable<Integer> integerList) {
		this.integerList = integerList;
	}

	public String[] getStringArray() {
		return stringArray;
	}

	public void setStringArray(final String[] stringArray) {
		this.stringArray = stringArray;
	}

	public void setStringList(final Iterable<String> stringList) {
		this.stringList = stringList;
	}

	public Customer getNestedCustomer() {
		return nestedCustomer;
	}

	public void setNestedCustomer(final Customer nestedCustomer) {
		this.nestedCustomer = nestedCustomer;
	}

	public Iterable<Customer> getNestedCustomers() {
		return nestedCustomers;
	}

	public void setNestedCustomers(final Iterable<Customer> nestedCustomers) {
		this.nestedCustomers = nestedCustomers;
	}

	public Iterable<String> getStringList() {
		return stringList;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof Customer)) {
			return false;
		}
		final Customer customer = (Customer) o;
		if (!customer.getId().equals(this.getId())) {
			return false;
		}
		if (!customer.getName().equals(this.getName())) {
			return false;
		}
		if (!customer.getSurname().equals(this.getSurname())) {
			return false;
		}
		if (customer.getAge() != this.getAge()) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Customer {id: " + id + ", name: " + name + ", surname: " + surname + ", age: " + age + "}";
	}

}
