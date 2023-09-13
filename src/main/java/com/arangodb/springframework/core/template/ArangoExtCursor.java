/*
 * DISCLAIMER
 *
 * Copyright 2017 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.springframework.core.template;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoIterator;
import com.arangodb.entity.CursorStats;
import com.arangodb.entity.CursorWarning;
import com.arangodb.springframework.core.mapping.event.AfterLoadEvent;
import com.arangodb.springframework.core.mapping.event.ArangoMappingEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 */
class ArangoExtCursor<T> implements ArangoCursor<T> {

    private final ArangoCursor<T> delegate;
    private final Class<T> type;
    private final ApplicationEventPublisher eventPublisher;

    public ArangoExtCursor(ArangoCursor<T> delegate, Class<T> type, ApplicationEventPublisher eventPublisher) {
        this.delegate = delegate;
        this.type = type;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public Integer getCount() {
        return delegate.getCount();
    }

    @Override
    public CursorStats getStats() {
        return delegate.getStats();
    }

    @Override
    public Collection<CursorWarning> getWarnings() {
        return delegate.getWarnings();
    }

    @Override
    public boolean isCached() {
        return delegate.isCached();
    }

    @Override
    public boolean isPotentialDirtyRead() {
        return delegate.isPotentialDirtyRead();
    }

    @Override
    public String getNextBatchId() {
        return delegate.getNextBatchId();
    }

    @Override
    public ArangoIterator<T> iterator() {
        return this;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public T next() {
        T result = delegate.next();
        potentiallyEmitEvent(result, AfterLoadEvent::new);
        return result;
    }

    private void potentiallyEmitEvent(Object o, Function<Object, ArangoMappingEvent<?>> constructor) {
        if (eventPublisher != null && o != null) {
            eventPublisher.publishEvent(constructor.apply(o));
        }
    }
}
