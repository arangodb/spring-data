package com.arangodb.springframework.core.template;

import com.arangodb.springframework.repository.StringIdTestEntity;

public class IdTypeStringTemplateTest extends AbstractTestEntityTemplateTest<StringIdTestEntity> {
    public IdTypeStringTemplateTest() {
        super(StringIdTestEntity.class);
    }

    @Override
    protected StringIdTestEntity createEntity() {
        return StringIdTestEntity.create();
    }
}
