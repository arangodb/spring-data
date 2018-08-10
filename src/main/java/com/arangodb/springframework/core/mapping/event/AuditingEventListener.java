/*
 * DISCLAIMER
 *
 * Copyright 2018 ArangoDB GmbH, Cologne, Germany
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

package com.arangodb.springframework.core.mapping.event;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.data.auditing.IsNewAwareAuditingHandler;

/**
 * @author Mark Vollmary
 *
 */
public class AuditingEventListener extends AbstractArangoEventListener<Object> {

	private final ObjectFactory<IsNewAwareAuditingHandler> auditingHandlerFactory;

	public AuditingEventListener(final ObjectFactory<IsNewAwareAuditingHandler> auditingHandlerFactory) {
		super();
		this.auditingHandlerFactory = auditingHandlerFactory;
	}

	@Override
	public void onBeforeSave(final BeforeSaveEvent<Object> event) {
		auditingHandlerFactory.getObject().markAudited(event.getSource());
	}

}
