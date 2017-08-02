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

import com.arangodb.springframework.annotation.*;
import org.springframework.data.annotation.Id;

import com.arangodb.springframework.annotation.Key;

/**
 * @author Mark Vollmary
 *
 */
@Document
public class Customer {

	@Id
	private String id;
	@Key
	private String key;
	private String name;
	private String surname;
	private int age;
	private Address address;

	private boolean alive;
	private int[] location = new int[2];
	private Iterable<Integer> integerList;
	private String[] stringArray;
	private Iterable<String> stringList;
	private String randomExistingField;

	@Ref
	private ShoppingCart shoppingCart;
	@Relations(edge = Owns.class)
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

	public Collection<Product> getOwns() { return owns; }

	public void setOwns(final Collection<Product> owns) {
		this.owns = owns;
	}

	public boolean isAlive() { return alive; }

	public void setAlive(boolean alive) { this.alive = alive; }

	public int[] getLocation() { return location; }

	public void setLocation(int[] location) { this.location = location; }

	public Iterable<Integer> getIntegerList() {	return integerList; }

	public void setIntegerList(Iterable<Integer> integerList) {	this.integerList = integerList;	}

	public String[] getStringArray() { return stringArray; }

	public void setStringArray(String[] stringArray) { this.stringArray = stringArray; }

	public void setStringList(Iterable<String> stringList) { this.stringList = stringList; }

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Customer)) return false;
		Customer customer = (Customer) o;
		if (!customer.getId().equals(this.getId())) return false;
		if (!customer.getName().equals(this.getName())) return false;
		if (!customer.getSurname().equals(this.getSurname())) return false;
		if (customer.getAge() != this.getAge()) return false;
		return true;
	}

	@Override
	public String toString() {
		return "Customer {id: " + id + ", name: " + name + ", surname: " + surname + ", age: " + age + "}";
	}
}
