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

package com.arangodb.springframework.core.convert;

import java.util.Collection;
import java.util.Map;

import com.arangodb.velocypack.VPackInstanceCreator;
import com.arangodb.velocypack.VPackModule;
import com.arangodb.velocypack.VPackSetupContext;

/**
 * @author Christian Lechner
 *
 */
public class DBEntityModule implements VPackModule {

	@SuppressWarnings("deprecation")
	@Override
	public <C extends VPackSetupContext<C>> void setup(final C context) {
		context.registerInstanceCreator(Map.class, new DBDocumentEntityInstantiator())
				.registerInstanceCreator(Collection.class, new DBCollectionEntityInstantiator())
				.registerDeserializer(DBEntity.class, new DBEntityDeserializer());
	}

	public static class DBDocumentEntityInstantiator implements VPackInstanceCreator<Map<?, ?>> {

		@Override
		public Map<?, ?> createInstance() {
			return new DBDocumentEntity();
		}

	}

	public static class DBCollectionEntityInstantiator implements VPackInstanceCreator<Collection<?>> {

		@SuppressWarnings("deprecation")
		@Override
		public Collection<?> createInstance() {
			return new DBCollectionEntity();
		}

	}

}
