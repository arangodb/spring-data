package com.arangodb.springframework.core.template;

import com.arangodb.springframework.repository.BigIntegerIdTestEntity;

public class IdTypeBigIntegerTemplateTest extends AbstractTestEntityTemplateTest<BigIntegerIdTestEntity> {
    public IdTypeBigIntegerTemplateTest() {
        super(BigIntegerIdTestEntity.class);
    }

    @Override
    protected BigIntegerIdTestEntity createEntity() {
        return BigIntegerIdTestEntity.create();
    }
}
