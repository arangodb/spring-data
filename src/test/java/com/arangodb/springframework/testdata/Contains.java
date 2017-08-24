package com.arangodb.springframework.testdata;

import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;

/**
 * Created by markmccormick on 24/08/2017.
 */
@Edge
public class Contains {

    @From
    Product from;

    @To
    Material to;

    public Contains() {
        super();
    }

    public Contains(final Product from, final Material to) {
        super();
        this.from = from;
        this.to = to;
    }

    public Product getFrom() {
        return from;
    }

    public void setFrom(final Product from) {
        this.from = from;
    }

    public Material getTo() {
        return to;
    }

    public void setTo(final Material to) {
        this.to = to;
    }
}
