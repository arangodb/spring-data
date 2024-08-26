package com.arangodb.springframework.core.template;

import com.arangodb.springframework.repository.LongIdTestEntity;

public class IdTypeLongTemplateTest extends AbstractTestEntityTemplateTest<LongIdTestEntity> {
    public IdTypeLongTemplateTest() {
        super(LongIdTestEntity.class);
    }

    @Override
    protected LongIdTestEntity createEntity() {
        return LongIdTestEntity.create();
    }
}
