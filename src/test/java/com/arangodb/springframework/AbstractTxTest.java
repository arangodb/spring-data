package com.arangodb.springframework;

import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.StreamTransactionOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;

public abstract class AbstractTxTest extends AbstractArangoTest {
    protected String tx;
    protected final DocumentCreateOptions insertOpts = new DocumentCreateOptions();
    protected final DocumentReadOptions findOpts = new DocumentReadOptions();
    protected final AqlQueryOptions queryOpts = new AqlQueryOptions().batchSize(1);
    private final boolean withinTx;

    protected AbstractTxTest(boolean withinTx, Class<?>... collections) {
        super(collections);
        this.withinTx = withinTx;
    }

    @BeforeEach
    void beginTx() {
        if (!withinTx) {
            return;
        }

        String[] txCols = Arrays.stream(collections)
                .map(it -> template.collection(it).name())
                .toArray(String[]::new);

        tx = db.beginStreamTransaction(new StreamTransactionOptions()
                .readCollections(txCols)
                .writeCollections(txCols)
        ).getId();

        insertOpts.streamTransactionId(tx);
        findOpts.streamTransactionId(tx);
        queryOpts
                .streamTransactionId(tx)
                .batchSize(1);
    }

    @AfterEach
    void abortTx() {
        if (!withinTx) {
            return;
        }
        db.abortStreamTransaction(tx);
    }

}
