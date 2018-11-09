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

package com.arangodb.springframework.core.mapping;

import java.util.Optional;

import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.TargetAwareIdentifierAccessor;
import org.springframework.util.Assert;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoIdPropertyIdentifierAccessor extends TargetAwareIdentifierAccessor {

	private final ArangoPersistentProperty arangoIdProperty;
	private final PersistentPropertyAccessor<?> accessor;

	public ArangoIdPropertyIdentifierAccessor(final ArangoPersistentEntity<?> entity, final Object target) {
		super(target);

		Assert.notNull(entity, "PersistentEntity must not be null!");
		final Optional<ArangoPersistentProperty> aip = entity.getArangoIdProperty();
		Assert.isTrue(aip.isPresent(), "PersistentEntity must have an arango identifier property!");
		Assert.notNull(target, "Target bean must not be null!");

		this.arangoIdProperty = aip.get();
		this.accessor = entity.getPropertyAccessor(target);
	}

	@Override
	public Object getIdentifier() {
		return accessor.getProperty(arangoIdProperty);
	}

}
