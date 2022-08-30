package com.arangodb.springframework.transaction;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.DbName;
import com.arangodb.entity.StreamTransactionEntity;
import com.arangodb.entity.StreamTransactionStatus;
import com.arangodb.model.StreamTransactionOptions;
import com.arangodb.model.TransactionCollectionOptions;
import com.arangodb.springframework.core.ArangoOperations;
import com.arangodb.springframework.repository.query.QueryTransactionBridge;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.lang.Nullable;
import org.springframework.transaction.*;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @After
    public void cleanupSync() {
        TransactionSynchronizationManager.unbindResourceIfPossible(DATABASE_NAME);
        TransactionSynchronizationManager.clear();
    }

    @Test
    public void getTransactionReturnsNewTransactionWithoutStreamTransaction() {
        TransactionStatus status = underTest.getTransaction(new DefaultTransactionAttribute());
        assertThat(status.isNewTransaction(), is(true));
        verify(driver).db(DATABASE_NAME);
        verify(bridge).setCurrentTransaction(any());
        ArangoTransactionResource resource = (ArangoTransactionResource) TransactionSynchronizationManager.getResource(DATABASE_NAME);
        assertThat(resource.getStreamTransactionId(), nullValue());
        assertThat(resource.getCollectionNames(), empty());
        assertThat(resource.isRollbackOnly(), is(false));
        verifyNoInteractions(database);
    }

    @Test
    public void nestedGetTransactionReturnsNewTransactionWithFormerCollections() {
        DefaultTransactionAttribute first = new DefaultTransactionAttribute();
        first.setLabels(Collections.singleton("foo"));
        TransactionStatus outer = underTest.getTransaction(first);
        DefaultTransactionAttribute second = new DefaultTransactionAttribute();
        second.setLabels(Collections.singleton("bar"));
        TransactionStatus inner = underTest.getTransaction(second);
        assertThat(inner.isNewTransaction(), is(true));
        ArangoTransactionObject transactionObject = getTransactionObject(inner);
        assertThat(transactionObject.getResource().getCollectionNames(), hasItems("foo", "bar"));
        verifyNoInteractions(database);
    }

    @Test(expected = UnexpectedRollbackException.class)
    public void innerRollbackCausesUnexpectedRollbackOnOuterCommit() {
        TransactionStatus outer = underTest.getTransaction(new DefaultTransactionAttribute());
        TransactionStatus inner = underTest.getTransaction(new DefaultTransactionAttribute());
        underTest.rollback(inner);
        underTest.commit(outer);
    }

    @Test
    public void getTransactionReturnsTransactionCreatesStreamTransactionWithAllCollectionsOnBridgeBeginCall() {
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setLabels(Collections.singleton("baz"));
        definition.setTimeout(20);
        TransactionStatus status = underTest.getTransaction(definition);
        beginTransaction("123", "foo", "bar");
        assertThat(status.isCompleted(), is(false));
        verify(database).beginStreamTransaction(optionsPassed.capture());
        assertThat(optionsPassed.getValue().getAllowImplicit(), is(true));
        assertThat(optionsPassed.getValue().getLockTimeout(), is(20));
        TransactionCollectionOptions collections = getCollections(optionsPassed.getValue());
        assertThat(collections.getRead(), nullValue());
        assertThat(collections.getExclusive(), nullValue());
        assertThat(collections.getWrite(), hasItems("baz", "foo", "bar"));
    }

    @Test
    public void nestedGetTransactionReturnsExistingTransactionWithFormerCollections() {
        DefaultTransactionAttribute first = new DefaultTransactionAttribute();
        first.setLabels(Collections.singleton("foo"));
        TransactionStatus outer = underTest.getTransaction(first);
        assertThat(outer.isNewTransaction(), is(true));

        beginTransaction("123", "foo", "bar");

        DefaultTransactionAttribute second = new DefaultTransactionAttribute();
        second.setLabels(Collections.singleton("bar"));
        TransactionStatus inner1 = underTest.getTransaction(second);
        assertThat(inner1.isNewTransaction(), is(false));
        ArangoTransactionObject tx1 = getTransactionObject(inner1);
        assertThat(tx1.getResource().getStreamTransactionId(), is("123"));
        underTest.commit(inner1);
        TransactionStatus inner2 = underTest.getTransaction(second);
        assertThat(inner2.isNewTransaction(), is(false));
        ArangoTransactionObject tx2 = getTransactionObject(inner1);
        assertThat(tx2.getResource().getStreamTransactionId(), is("123"));
        underTest.commit(inner2);
        underTest.commit(outer);
        verify(database).commitStreamTransaction("123");
        assertThat(TransactionSynchronizationManager.getResource(DATABASE_NAME), nullValue());
    }

    @Test
    public void getTransactionWithMultipleBridgeCallsWorksForKnownCollections() {
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setLabels(Collections.singleton("baz"));
        definition.setTimeout(20);
        underTest.getTransaction(definition);
        beginTransaction("123", "foo");
        beginPassed.getValue().apply(Arrays.asList("foo", "baz"));
        verify(database).beginStreamTransaction(optionsPassed.capture());
        TransactionCollectionOptions collections = getCollections(optionsPassed.getValue());
        assertThat(collections.getWrite(), hasItems("baz", "foo"));
    }

    @Test
    public void getTransactionWithMultipleBridgeCallsIgnoresAdditionalCollections() {
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setLabels(Collections.singleton("bar"));
        definition.setTimeout(20);
        TransactionStatus state = underTest.getTransaction(definition);
        beginTransaction("123", "foo");
        beginPassed.getValue().apply(Collections.singletonList("baz"));
        assertThat(getTransactionObject(state).getResource().getCollectionNames(), hasItems("foo", "bar"));
        assertThat(getTransactionObject(state).getResource().getCollectionNames(), not(hasItem("baz")));
    }

    @Test(expected = InvalidIsolationLevelException.class)
    public void getTransactionThrowsInvalidIsolationLevelExceptionForIsolationSerializable() {
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        underTest.getTransaction(definition);
    }

    private void beginTransaction(String id, String... collectionNames) {
        when(streamTransaction.getId())
                .thenReturn(id);
        when(database.beginStreamTransaction(any()))
                .thenReturn(streamTransaction);
        when(database.getStreamTransaction(any()))
                .thenReturn(streamTransaction);
        when(streamTransaction.getStatus())
                .thenReturn(StreamTransactionStatus.running);
        verify(bridge).setCurrentTransaction(beginPassed.capture());
        beginPassed.getValue().apply(Arrays.asList(collectionNames));
    }

    @Nullable
    private ArangoTransactionObject getTransactionObject(TransactionStatus status) {
        if (status instanceof DefaultTransactionStatus) {
            return (ArangoTransactionObject) ((DefaultTransactionStatus) status).getTransaction();
        }
        return null;
    }

    private TransactionCollectionOptions getCollections(StreamTransactionOptions options) {
        return (TransactionCollectionOptions) forDirectFieldAccess(options).getPropertyValue("collections");
    }
}
