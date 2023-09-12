package com.arangodb.springframework.core;

import org.springframework.dao.DataRetrievalFailureException;

public class DocumentNotFoundException extends DataRetrievalFailureException {
    public DocumentNotFoundException(String msg) {
        super(msg);
    }

    public DocumentNotFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
