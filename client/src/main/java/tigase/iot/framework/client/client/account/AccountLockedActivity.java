/*
 * AccountLockedActivity.java
 *
 * Tigase IoT Framework
 * Copyright (C) 2011-2018 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package tigase.iot.framework.client.client.account;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import tigase.iot.framework.client.client.ClientFactory;

public class AccountLockedActivity extends AbstractActivity {

		private final ClientFactory factory;

	public AccountLockedActivity(AccountLockedPlace place, ClientFactory factory_) {
			this.factory = factory_;
		}

		@Override
		public void start(AcceptsOneWidget panel, EventBus eventBus) {
			AccountLockedView view = factory.accountLockedView();
			panel.setWidget(view.asWidget());
		}
}
