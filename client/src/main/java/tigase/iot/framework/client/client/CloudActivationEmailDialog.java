/*
 * CloudActivationEmailDialog.java
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

package tigase.iot.framework.client.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import tigase.iot.framework.client.Hub;
import tigase.iot.framework.client.client.ui.MessageDialog;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;

public class CloudActivationEmailDialog {

	private final DialogBox box;

	public CloudActivationEmailDialog(ClientFactory factory) {
		box = new DialogBox(true) {
			@Override
			public void hide(boolean autoClosed) {
				super.hide(autoClosed);
			}
		};
		box.setStylePrimaryName("dialog-window");
		box.setGlassEnabled(true);

		VerticalPanel panel = new VerticalPanel();
		panel.setSize("100%", "100%");

		panel.add(new Label("Please enter your email to create an account for connection to IoT One Cloud"));

		TextBox emailField = new TextBox();
		emailField.setSize("100%", "100%");
		panel.add(emailField);

		Button submit = new Button("Enable");
		submit.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent clickEvent) {
				box.hide();

				String email = emailField.getText();
				if (email == null || email.trim().isEmpty()) {
				} else {
					try {
						factory.hub().updateCloudSettings(true, email, new Hub.CompletionHandler() {
							@Override
							public void onResult(XMPPException.ErrorCondition errorCondition) {
								if (errorCondition != null) {
									new MessageDialog("Error",
													  "Could not update settings. Please try again later.")
											.show();
								}
							}
						});
					} catch (JaxmppException ex) {
						new MessageDialog("Error",
										  "Could not update settings. Please try again later.")
								.show();
					}
				}
			}
		});
		panel.add(submit);
		submit.getElement().getParentElement().addClassName("context-menu");

		box.setWidget(panel);
	}

	public void show() {
		box.center();
	}

}
