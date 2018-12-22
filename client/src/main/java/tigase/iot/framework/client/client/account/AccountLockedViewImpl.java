/*
 * AccountLockedViewImpl.java
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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import tigase.iot.framework.client.client.ClientFactory;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;

public class AccountLockedViewImpl
		extends Composite
		implements AccountLockedView {

	private final ClientFactory factory;

	public AccountLockedViewImpl(ClientFactory factory) {
		this.factory = factory;


		AbsolutePanel panel = new AbsolutePanel();
		panel.setStyleName("auth");

		FlowPanel flowPanel = new FlowPanel();

		Label header = new Label("Not enabled");
		header.setStyleName("auth-header");
		flowPanel.add(header);

		Label messageLabel = new Label("This account is not enabled on the Tigase IoT Hub. You must use a different account to log in and then enable this account.", true);
		messageLabel.setStyleName("auth-label");
		flowPanel.add(messageLabel);

		Button logoutButton = new Button("Logout");
		logoutButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				try {
					factory.jaxmpp().disconnect();
				} catch (JaxmppException e) {
					e.printStackTrace();
				}
			}
		});
		logoutButton.setStyleName("auth-button");
		flowPanel.add(logoutButton);

		panel.add(flowPanel);
		initWidget(panel);
	}

}
