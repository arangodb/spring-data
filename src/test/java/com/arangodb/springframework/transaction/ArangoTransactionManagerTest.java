package com.arangodb.springframework.transaction;

import com.arangodb.ArangoDatabase;
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
import static org.mockito.Mockito.*;
import static org.springframework.beans.PropertyAccessorFactory.forDirectFieldAccess;

@RunWith(MockitoJUnitRunner.class)
public class ArangoTransactionManagerTest {

    @Mock
    private ArangoOperations operations;
    @Mock
    private QueryTransactionBridge bridge;
    @InjectMocks
    private ArangoTransactionManager underTest;
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
        when(operations.db())
                .thenReturn(database);
    }

    @After
    public void cleanupSync() {
        TransactionSynchronizationManager.unbindResourceIfPossible(underTest);
        TransactionSynchronizationManager.clear();
    }

    @Test
    public void getTransactionReturnsNewTransactionWithoutStreamTransaction() {
        TransactionStatus status = underTest.getTransaction(createTransactionAttribute("test"));
        assertThat(status.isNewTransaction(), is(true));
        verify(bridge).setCurrentTransaction(any());
        ArangoTransactionHolder resource = (ArangoTransactionHolder) TransactionSynchronizationManager.getResource(underTest);
        assertThat(resource.getStreamTransactionId(), nullValue());
        assertThat(resource.getCollectionNames(), empty());
        assertThat(resource.isRollbackOnly(), is(false));
        verifyNoInteractions(database);
    }

    @Test
    public void innerGetTransactionIsNotNewTransactionIncludingFormerCollections() {
        TransactionStatus outer = underTest.getTransaction(createTransactionAttribute("outer", "foo"));
        TransactionStatus inner = underTest.getTransaction(createTransactionAttribute("inner", "bar"));
        assertThat(inner.isNewTransaction(), is(false));
        ArangoTransactionObject transactionObject = getTransactionObject(inner);
        assertThat(transactionObject.getHolder().getCollectionNames(), hasItems("foo", "bar"));
        verifyNoInteractions(database);
    }

    @Test(expected = UnexpectedRollbackException.class)
    public void innerRollbackCausesUnexpectedRollbackOnOuterCommit() {
        TransactionStatus outer = underTest.getTransaction(createTransactionAttribute("outer"));
        TransactionStatus inner = underTest.getTransaction(createTransactionAttribute("inner"));
        underTest.rollback(inner);
        try {
            underTest.commit(outer);
        } finally {
            assertThat(TransactionSynchronizationManager.getResource(underTest), nullValue());
        }
    }

    @Test
    public void getTransactionReturnsTransactionCreatesStreamTransactionWithAllCollectionsOnBridgeBeginCall() {
        DefaultTransactionAttribute definition = createTransactionAttribute("timeout", "baz");
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
        TransactionStatus outer = underTest.getTransaction(createTransactionAttribute("outer", "foo"));
        assertThat(outer.isNewTransaction(), is(true));

        beginTransaction("123", "foo", "bar");
        when(streamTransaction.getStatus())
                .thenReturn(StreamTransactionStatus.running);

        DefaultTransactionAttribute second = createTransactionAttribute("inner", "bar");
        TransactionStatus inner1 = underTest.getTransaction(second);
        assertThat(inner1.isNewTransaction(), is(false));
        ArangoTransactionObject tx1 = getTransactionObject(inner1);
        assertThat(tx1.getHolder().getStreamTransactionId(), is("123"));
        underTest.commit(inner1);
        TransactionStatus inner2 = underTest.getTransaction(second);
        assertThat(inner2.isNewTransaction(), is(false));
        ArangoTransactionObject tx2 = getTransactionObject(inner1);
        assertThat(tx2.getHolder().getStreamTransactionId(), is("123"));
        underTest.commit(inner2);
        underTest.commit(outer);
        verify(database).commitStreamTransaction("123");
        assertThat(TransactionSynchronizationManager.getResource(underTest), nullValue());
    }

    @Test
    public void getTransactionForPropagationSupportsWithoutExistingCreatesDummyTransaction() {
        DefaultTransactionAttribute supports = createTransactionAttribute("test", "foo");
        supports.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
        TransactionStatus empty = underTest.getTransaction(supports);
        assertThat(empty.isNewTransaction(), is(false));
        underTest.commit(empty);
        verifyNoInteractions(database);
        assertThat(TransactionSynchronizationManager.getResource(underTest), nullValue());
    }

    @Test
    public void getTransactionForPropagationSupportsWithExistingCreatesInner() {
        TransactionStatus outer = underTest.getTransaction(createTransactionAttribute("outer", "foo"));
        DefaultTransactionAttribute supports = createTransactionAttribute("supports", "bar");
        supports.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
        TransactionStatus inner = underTest.getTransaction(supports);
        assertThat(inner.isNewTransaction(), is(false));
        underTest.commit(inner);
        ArangoTransactionObject transactionObject = getTransactionObject(inner);
        assertThat(TransactionSynchronizationManager.getResource(underTest), is(transactionObject.getHolder()));
        verifyNoInteractions(database);
    }

    @Test
    public void getTransactionWithMultipleBridgeCallsWorksForKnownCollections() {
        underTest.getTransaction(createTransactionAttribute("test", "baz"));
        beginTransaction("123", "foo");
        beginPassed.getValue().apply(Arrays.asList("foo", "baz"));
        verify(database).beginStreamTransaction(optionsPassed.capture());
        TransactionCollectionOptions collections = getCollections(optionsPassed.getValue());
        assertThat(collections.getWrite(), hasItems("baz", "foo"));
    }

    @Test
    public void getTransactionWithMultipleBridgeCallsIgnoresAdditionalCollections() {
        TransactionStatus state = underTest.getTransaction(createTransactionAttribute("test", "bar"));
        beginTransaction("123", "foo");
        beginPassed.getValue().apply(Collections.singletonList("baz"));
        assertThat(getTransactionObject(state).getHolder().getCollectionNames(), hasItems("foo", "bar"));
        assertThat(getTransactionObject(state).getHolder().getCollectionNames(), not(hasItem("baz")));
    }

    @Test(expected = InvalidIsolationLevelException.class)
    public void getTransactionThrowsInvalidIsolationLevelExceptionForIsolationSerializable() {
        DefaultTransactionAttribute definition = createTransactionAttribute("serializable");
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        underTest.getTransaction(definition);
    }

    private void beginTransaction(String id, String... collectionNames) {
        when(streamTransaction.getId())
                .thenReturn(id);
        when(database.beginStreamTransaction(any()))
                .thenReturn(streamTransaction);
        lenient().when(database.getStreamTransaction(any()))
                .thenReturn(streamTransaction);
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

    private static DefaultTransactionAttribute createTransactionAttribute(String name, String... collections) {
        DefaultTransactionAttribute transactionAttribute = new DefaultTransactionAttribute();
        transactionAttribute.setName(name);
        transactionAttribute.setLabels(Arrays.asList(collections));
        return transactionAttribute;
    }
}
