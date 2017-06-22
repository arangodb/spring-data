/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
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

package com.arangodb.springframework.core;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.arangodb.ArangoDB;

/**
 * @author Mark - mark at arangodb.com
 *
 */
public class ArangoFactoryBean extends AbstractFactoryBean<ArangoDB.Builder> {

	@Override
	public Class<?> getObjectType() {
		return ArangoDB.Builder.class;
	}

	@Override
	protected ArangoDB.Builder createInstance() throws Exception {
		return new ArangoDB.Builder();
	}

}
