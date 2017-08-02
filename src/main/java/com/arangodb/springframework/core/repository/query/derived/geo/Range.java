package com.arangodb.springframework.core.repository.query.derived.geo;

/**
 * Created by F625633 on 21/07/2017.
 */
public class Range<T> {

	private T lowerBound;
	private T upperBound;

	public Range(T lowerBound, T upperBound) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public T getLowerBound() {
		return lowerBound;
	}

	public T getUpperBound() {
		return upperBound;
	}
}
