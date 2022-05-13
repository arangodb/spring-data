package com.arangodb.springframework.repository.query;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;

public class QueryTransactionBridgeTest {

    private QueryTransactionBridge underTest = new QueryTransactionBridge();

    @Test
    public void getCurrentTransactionInitiallyReturnsNull() {
        assertThat(underTest.getCurrentTransaction(Collections.singleton("test")), Matchers.nullValue());
    }

    @Test
    public void setCurrentTransactionIsAppliedOnGetCurrentTransaction() {
        underTest.setCurrentTransaction(collections -> collections.iterator().next());
        assertThat(underTest.getCurrentTransaction(Collections.singleton("test")), Matchers.is("test"));
    }

    @After
    public void cleanup() {
        underTest.clearCurrentTransaction();
    }
}
