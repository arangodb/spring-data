package com.arangodb.springframework.repository.query;

import com.arangodb.springframework.repository.ArangoRepository;
import com.arangodb.springframework.testdata.Product;

public interface ProductRepository extends ArangoRepository<Product, String> {
}
