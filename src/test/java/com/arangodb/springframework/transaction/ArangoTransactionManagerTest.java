package com.arangodb.springframework.transaction;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.DbName;
import com.arangodb.entity.StreamTransactionEntity;
import com.arangodb.model.StreamTransactionOptions;
import com.arangodb.model.TransactionCollectionOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.repository.query.QueryTransactionBridge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.beans.PropertyAccessorFactory.forDirectFieldAccess;

@RunWith(MockitoJUnitRunner.class)
public class ArangoTransactionManagerTest {

    private static final DbName DATABASE_NAME = DbName.of("test");

    @Mock
    private ArangoOperations operations;
    @Mock
    private QueryTransactionBridge bridge;
    @InjectMocks
    private ArangoTransactionManager underTest;
    @Mock
    private ArangoDB driver;
    @Mock
    private ArangoDatabase database;
    @Mock
    private StreamTransactionEntity streamTransaction;
    @Captor
    private ArgumentCaptor<Function<Collection<String>, String>> beginPassed;
    @Captor
    private ArgumentCaptor<StreamTransactionOptions> optionsPassed;

    @Before
    public void setupMocks() {
        when(operations.getDatabaseName())
                .thenReturn(DATABASE_NAME);
        when(operations.driver())
                .thenReturn(driver);
        when(driver.db(any(DbName.class)))
                .thenReturn(database);
    }

    @Test
    public void getTransactionReturnsNewTransactionWithoutStreamTransaction() {
        TransactionStatus transaction = underTest.getTransaction(new DefaultTransactionAttribute());
        assertThat(transaction.isNewTransaction(), is(true));
        verify(driver).db(DATABASE_NAME);
        verify(bridge).setCurrentTransaction(any());
        verifyNoInteractions(database);
    }

    @Test
    public void getTransactionReturnsTransactionCreatesStreamTransactionWithAllCollectionsOnBridgeBeginCall() {
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setLabels(Collections.singleton("baz"));
        definition.setTimeout(20);
        TransactionStatus transaction = underTest.getTransaction(definition);
        when(streamTransaction.getId())
                .thenReturn("123");
        when(database.beginStreamTransaction(any()))
                .thenReturn(streamTransaction);
        verify(bridge).setCurrentTransaction(beginPassed.capture());
        beginPassed.getValue().apply(Arrays.asList("foo", "bar"));
        assertThat(transaction.isCompleted(), is(false));
        verify(database).beginStreamTransaction(optionsPassed.capture());
        assertThat(optionsPassed.getValue().getAllowImplicit(), is(true));
        assertThat(optionsPassed.getValue().getLockTimeout(), is(20));
        TransactionCollectionOptions collections = getCollections(optionsPassed.getValue());
        assertThat(collections.getRead(), nullValue());
        assertThat(collections.getExclusive(), nullValue());
        assertThat(collections.getWrite(), hasItems("baz", "foo", "bar"));
    }

    @Test
    public void getTransactionWithMultipleBridgeCallsWorksForKnownCollections() {
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setLabels(Collections.singleton("baz"));
        definition.setTimeout(20);
        underTest.getTransaction(definition);
        when(streamTransaction.getId())
                .thenReturn("123");
        when(database.beginStreamTransaction(any()))
                .thenReturn(streamTransaction);
        verify(bridge).setCurrentTransaction(beginPassed.capture());
        beginPassed.getValue().apply(Collections.singletonList("foo"));
        beginPassed.getValue().apply(Arrays.asList("foo", "baz"));
        verify(database).beginStreamTransaction(optionsPassed.capture());
        TransactionCollectionOptions collections = getCollections(optionsPassed.getValue());
        assertThat(collections.getWrite(), hasItems("baz", "foo"));
    }

    @Test(expected = IllegalTransactionStateException.class)
    public void getTransactionWithMultipleBridgeCallsFailsForAdditionalCollection() {
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setLabels(Collections.singleton("baz"));
        definition.setTimeout(20);
        underTest.getTransaction(definition);
        when(streamTransaction.getId())
                .thenReturn("123");
        when(database.beginStreamTransaction(any()))
                .thenReturn(streamTransaction);
        verify(bridge).setCurrentTransaction(beginPassed.capture());
        beginPassed.getValue().apply(Collections.singletonList("foo"));
        beginPassed.getValue().apply(Collections.singletonList("bar"));
    }

    @Test(expected = InvalidIsolationLevelException.class)
    public void getTransactionThrowsInvalidIsolationLevelExceptionForIsolationSerializable() {
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        underTest.getTransaction(definition);
    }

    private TransactionCollectionOptions getCollections(StreamTransactionOptions options) {
        return (TransactionCollectionOptions) forDirectFieldAccess(options).getPropertyValue("collections");
    }
}
