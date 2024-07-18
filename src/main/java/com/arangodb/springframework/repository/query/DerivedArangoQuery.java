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

package com.arangodb.springframework.repository.query;

import com.arangodb.entity.IndexEntity;
import com.arangodb.entity.IndexType;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.repository.query.derived.BindParameterBinding;
import com.arangodb.springframework.repository.query.derived.DerivedQueryCreator;
import org.springframework.data.repository.query.parser.PartTree;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Audrius Malele
 * @author Mark McCormick
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public class DerivedArangoQuery extends AbstractArangoQuery {

	private final PartTree tree;
	private final List<String> geoFields;

	public DerivedArangoQuery(final ArangoQueryMethod method, final ArangoOperations operations,
							  final QueryTransactionBridge transactionBridge) {
		super(method, operations, transactionBridge);
		tree = new PartTree(method.getName(), domainClass);
		geoFields = getGeoFields();
	}

	@Override
	protected QueryWithCollections createQuery(
            final ArangoParameterAccessor accessor,
		final Map<String, Object> bindVars,
		final AqlQueryOptions options) {

		return new DerivedQueryCreator(mappingContext, domainClass, tree, accessor, new BindParameterBinding(bindVars),
				geoFields).createQuery();
	}

	@Override
	protected boolean isCountQuery() {
		return tree.isCountProjection();
	}

	@Override
	protected boolean isExistsQuery() {
		return tree.isExistsProjection();
	}

	private List<String> getGeoFields() {
		final List<String> geoFields = new LinkedList<>();
		if (method.isGeoQuery()) {
			for (final IndexEntity index : operations.collection(domainClass).getIndexes()) {
				final IndexType type = index.getType();
				if (type == IndexType.geo || type == IndexType.geo1 || type == IndexType.geo2) {
					geoFields.addAll(index.getFields());
				}
			}
		}
		return geoFields;
	}

}
