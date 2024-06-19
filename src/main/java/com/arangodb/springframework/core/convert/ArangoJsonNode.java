package com.arangodb.springframework.core.convert;

import com.arangodb.springframework.core.mapping.TransactionMappingContext;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * JsonNode with TransactionMappingContext
 */
public record ArangoJsonNode(
        JsonNode value,
        TransactionMappingContext transactionContext
) {
}
