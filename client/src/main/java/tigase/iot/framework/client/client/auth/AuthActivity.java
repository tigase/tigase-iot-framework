/*
 * AuthActivity.java
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.auth;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import tigase.iot.framework.client.client.ClientFactory;

/**
 *
 * @author andrzej
 */
public class AuthActivity extends AbstractActivity {

	private final ClientFactory factory;

	public AuthActivity(AuthPlace place, ClientFactory factory_) {
		this.factory = factory_;
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		AuthView view = factory.authView();
		view.refresh();
		panel.setWidget(view.asWidget());
	}

}
