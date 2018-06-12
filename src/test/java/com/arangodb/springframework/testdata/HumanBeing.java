package com.arangodb.springframework.testdata;

import java.util.Collection;

import org.springframework.data.annotation.Id;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.HashIndex;
import com.arangodb.springframework.annotation.Relations;

@Document
@HashIndex(fields = { "name", "surname" }, unique = true)
public class HumanBeing {
	@Id
	private String id;

	private String name;
	private String surname;
	private boolean alive;
	private Integer age;
	@Relations(edges = ChildOf.class, lazy = true)
	private Collection<HumanBeing> children;

	public HumanBeing() {
	}

	/**
	 * @param name
	 * @param surname
	 * @param alive
	 */
	public HumanBeing(String name, String surname, boolean alive) {
		super();
		this.name = name;
		this.surname = surname;
		this.alive = alive;
	}

	/**
	 * @param name
	 * @param surname
	 * @param alive
	 * @param age
	 */
	public HumanBeing(String name, String surname, boolean alive, Integer age) {
		super();
		this.name = name;
		this.surname = surname;
		this.alive = alive;
		this.age = age;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the surname
	 */
	public String getSurname() {
		return surname;
	}

	/**
	 * @param surname the surname to set
	 */
	public void setSurname(String surname) {
		this.surname = surname;
	}

	/**
	 * @return the alive
	 */
	public boolean isAlive() {
		return alive;
	}

	/**
	 * @param alive the alive to set
	 */
	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	/**
	 * @return the age
	 */
	public Integer getAge() {
		return age;
	}

	/**
	 * @param age the age to set
	 */
	public void setAge(Integer age) {
		this.age = age;
	}

	/**
	 * @param childs the childs to set
	 */
	public void setChildren(Collection<HumanBeing> children) {
		this.children = children;
	}

	/**
	 * @return the childs
	 */
	public Collection<HumanBeing> getChildren() {
		return children;
	}

	@Override
	public String toString() {
		return "Character [id=" + id + ", name=" + name + ", surname=" + surname + ", alive=" + alive + ", age=" +age+ "]";
	}
}
