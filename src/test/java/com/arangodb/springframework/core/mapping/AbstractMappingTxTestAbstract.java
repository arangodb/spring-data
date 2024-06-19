package com.arangodb.springframework.core.mapping;

import com.arangodb.springframework.AbstractTxTest;
import com.arangodb.springframework.core.mapping.testdata.BasicEdgeLazyTestEntity;
import com.arangodb.springframework.core.mapping.testdata.BasicEdgeTestEntity;
import com.arangodb.springframework.core.mapping.testdata.BasicTestEntity;

import java.util.ArrayList;
import java.util.Collections;

public class AbstractMappingTxTestAbstract extends AbstractTxTest {

    private static Class<?>[] enrichCollections(final Class<?>... collections) {
        ArrayList<Class<?>> classes = new ArrayList<>();
        Collections.addAll(classes, collections);
        classes.add(BasicTestEntity.class);
        classes.add(BasicEdgeTestEntity.class);
        classes.add(BasicEdgeLazyTestEntity.class);
        return classes.toArray(new Class[0]);
    }

    public AbstractMappingTxTestAbstract(boolean withinTx, final Class<?>... collections) {
        super(withinTx, enrichCollections(collections));
    }

}
