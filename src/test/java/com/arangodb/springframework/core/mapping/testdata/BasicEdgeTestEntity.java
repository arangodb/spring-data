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

package com.arangodb.springframework.core.mapping.testdata;

import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;

/**
 * @author Mark Vollmary
 *
 */
@Edge
public class BasicEdgeTestEntity extends BasicTestEntity {

	@From
	public BasicTestEntity from;
	@To
	public BasicTestEntity to;

	public BasicEdgeTestEntity() {
		super();
	}

	public BasicEdgeTestEntity(final BasicTestEntity from, final BasicTestEntity to) {
		super();
		this.from = from;
		this.to = to;
	}

	public BasicTestEntity getFrom() {
		return from;
	}

	public void setFrom(final BasicTestEntity from) {
		this.from = from;
	}

	public BasicTestEntity getTo() {
		return to;
	}

	public void setTo(final BasicTestEntity to) {
		this.to = to;
	}

}