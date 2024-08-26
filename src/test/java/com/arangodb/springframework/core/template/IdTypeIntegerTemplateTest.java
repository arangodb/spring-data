package com.arangodb.springframework.core.template;

import com.arangodb.springframework.repository.IntegerIdTestEntity;

public class IdTypeIntegerTemplateTest extends AbstractTestEntityTemplateTest<IntegerIdTestEntity> {
    public IdTypeIntegerTemplateTest() {
        super(IntegerIdTestEntity.class);
    }

    @Override
    protected IntegerIdTestEntity createEntity() {
        return IntegerIdTestEntity.create();
    }
}
