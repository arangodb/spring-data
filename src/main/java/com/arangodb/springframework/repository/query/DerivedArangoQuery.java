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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.data.repository.query.parser.PartTree;

import com.arangodb.entity.IndexEntity;
import com.arangodb.entity.IndexType;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.core.mapping.ArangoMappingContext;
import com.arangodb.springframework.repository.query.derived.DerivedQueryCreator;

/**
 * 
 * @author Andrew Fleming
 * @author Audrius Malele
 * @author Mark Vollmary
 * @author Christian Lechner
 */
public class DerivedArangoQuery extends AbstractArangoQuery {

	private final PartTree tree;
	private final ArangoMappingContext context;
	private final List<String> geoFields;

	public DerivedArangoQuery(ArangoQueryMethod method, ArangoOperations operations) {
		super(method, operations);
		this.tree = new PartTree(method.getName(), this.domainClass);
		this.context = (ArangoMappingContext) operations.getConverter().getMappingContext();
		this.geoFields = getGeoFields();
	}

	@Override
	protected String createQuery(
		final ArangoParameterAccessor accessor,
		final Map<String, Object> bindVars,
		final AqlQueryOptions options) {

		if (method.isPageQuery()) {
			options.fullCount(true);
		}

		return new DerivedQueryCreator(context, domainClass, tree, accessor, bindVars, geoFields,
				operations.getVersion().getVersion().compareTo("3.2.0") < 0).createQuery();
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
			for (IndexEntity index : operations.collection(domainClass).getIndexes()) {
				if ((index.getType() == IndexType.geo1)) {
					geoFields.addAll(index.getFields());
				}
			}
		}
		return geoFields;
	}

}
