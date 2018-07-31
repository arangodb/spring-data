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

import java.util.ArrayList;

import org.springframework.stereotype.Component;

import com.arangodb.springframework.testdata.Customer;

@Component
public class CustomerEventListener extends AbstractArangoEventListener<Customer> {

	public final ArrayList<AfterLoadEvent<Customer>> afterLoadEvents = new ArrayList<>();
	public final ArrayList<BeforeSaveEvent<Customer>> beforeSaveEvents = new ArrayList<>();
	public final ArrayList<AfterSaveEvent<Customer>> afterSaveEvents = new ArrayList<>();
	public final ArrayList<BeforeDeleteEvent<Customer>> beforeDeleteEvents = new ArrayList<>();
	public final ArrayList<AfterDeleteEvent<Customer>> afterDeleteEvents = new ArrayList<>();

	@Override
	public void onAfterLoad(final AfterLoadEvent<Customer> event) {
		afterLoadEvents.add(event);
	}

	@Override
	public void onBeforeSave(final BeforeSaveEvent<Customer> event) {
		beforeSaveEvents.add(event);
	}

	@Override
	public void onAfterSave(final AfterSaveEvent<Customer> event) {
		afterSaveEvents.add(event);
	}

	@Override
	public void onBeforeDelete(final BeforeDeleteEvent<Customer> event) {
		beforeDeleteEvents.add(event);
	}

	@Override
	public void onAfterDelete(final AfterDeleteEvent<Customer> event) {
		afterDeleteEvents.add(event);
	}

}