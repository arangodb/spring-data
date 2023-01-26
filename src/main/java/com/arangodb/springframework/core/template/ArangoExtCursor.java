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

import com.arangodb.*;
import com.arangodb.entity.CursorWarning;
import com.arangodb.springframework.core.convert.ArangoConverter;
import com.arangodb.springframework.core.mapping.event.AfterLoadEvent;
import com.arangodb.springframework.core.mapping.event.ArangoMappingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Mark Vollmary
 * @author Christian Lechner
 */
class ArangoExtCursor<T> implements ArangoCursor<T> {

    private final ArangoCursor<JsonNode> delegate;
    private final Class<T> type;
    private final ArangoConverter converter;
    private final ApplicationEventPublisher eventPublisher;

    public ArangoExtCursor(ArangoCursor<JsonNode> delegate, Class<T> type, ArangoConverter converter, ApplicationEventPublisher eventPublisher) {
        this.delegate = delegate;
        this.type = type;
        this.converter = converter;
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
    public Map<String, Object> getStats() {
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
    public List<T> asListRemaining() {
        final List<T> remaining = new ArrayList<>();
        while (hasNext()) {
            remaining.add(next());
        }
        return remaining;
    }

    @Override
    public boolean isPotentialDirtyRead() {
        return delegate.isPotentialDirtyRead();
    }

    @Override
    public ArangoIterator<T> iterator() {
        return this;
    }

    @Override
    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
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
        final T result = converter.read(type, delegate.next());
        if (result != null) {
            potentiallyEmitEvent(new AfterLoadEvent<>(result));
        }
        return result;
    }

    private void potentiallyEmitEvent(final ArangoMappingEvent<?> event) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(event);
        }
    }
}
