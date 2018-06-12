package com.arangodb.springframework.testdata;

import org.springframework.data.annotation.Id;

import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;

@Edge
public class ChildOf {
	@Id
	private String id;

	@From
	private HumanBeing child;

	@To
	private HumanBeing parent;

	public ChildOf() {
	}
	
	public ChildOf(final HumanBeing child, final HumanBeing parent) {
		super();
		this.child = child;
		this.parent = parent;
	}

	@Override
	public String toString() {
		return "ChildOf [id=" + id + ", child=" + child + ", parent=" + parent + "]";
	}

	// setter & getter
	
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
	 * @return the child
	 */
	public HumanBeing getChild() {
		return child;
	}

	/**
	 * @param child the child to set
	 */
	public void setChild(HumanBeing child) {
		this.child = child;
	}

	/**
	 * @return the parent
	 */
	public HumanBeing getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(HumanBeing parent) {
		this.parent = parent;
	}
}
