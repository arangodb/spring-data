package com.arangodb.springframework.core.template;

import com.arangodb.springframework.repository.UuidIdTestEntity;

public class IdTypeUUIDTemplateTest extends AbstractTestEntityTemplateTest<UuidIdTestEntity> {
    public IdTypeUUIDTemplateTest() {
        super(UuidIdTestEntity.class);
    }

    @Override
    protected UuidIdTestEntity createEntity() {
        return UuidIdTestEntity.create();
    }
}
