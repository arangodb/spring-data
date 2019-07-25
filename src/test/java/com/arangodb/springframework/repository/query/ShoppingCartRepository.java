package com.arangodb.springframework.repository.query;

import com.arangodb.springframework.repository.ArangoRepository;
import com.arangodb.springframework.testdata.ShoppingCart;

public interface ShoppingCartRepository extends ArangoRepository<ShoppingCart, String> {
}
