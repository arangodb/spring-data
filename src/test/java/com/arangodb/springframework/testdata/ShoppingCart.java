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

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.data.annotation.Id;

import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Ref;

/**
 * @author Mark Vollmary
 *
 */
@Document("shopping-cart")
public class ShoppingCart {

	@Id
	private String id;
	@Ref
	private Collection<Product> products;

	public ShoppingCart() {
		super();
		products = new ArrayList<>();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Collection<Product> getProducts() {
		return products;
	}

	public void setProducts(final Collection<Product> products) {
		this.products = products;
	}

}
