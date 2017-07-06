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

import com.arangodb.springframework.annotation.Edge;

/**
 * @author Mark Vollmary
 *
 */
@Edge
public class Owns {

	private String _from;
	private String _to;

	public Owns() {
		super();
	}

	public Owns(final String _from, final String _to) {
		super();
		this._from = _from;
		this._to = _to;
	}

	public String get_from() {
		return _from;
	}

	public void set_from(final String _from) {
		this._from = _from;
	}

	public String get_to() {
		return _to;
	}

	public void set_to(final String _to) {
		this._to = _to;
	}

}
