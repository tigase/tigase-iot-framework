/*
 * AddDeviceDlg.java
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
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.client.ui.Form;
import tigase.iot.framework.client.client.ui.MessageDialog;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.Action;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.AdHocCommansModule;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.State;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author andrzej
 */
public class AddDeviceDlg {

	private final ClientFactory factory;
	private final JID deviceHostJid;
	private final Form form;
	private final DialogBox dialog;

	private final Button advancedButton;
	private final Button submitButton;

	public AddDeviceDlg(ClientFactory factory, JID deviceHostJid) {
		this.factory = factory;
		this.deviceHostJid = deviceHostJid;

		FlowPanel panel = new FlowPanel();
		panel.setStylePrimaryName("context-menu");

		form = new Form();

		dialog = new DialogBox(true, true);
		dialog.setStylePrimaryName("dialog-window");
		dialog.setTitle("Add device");

		panel.add(form);

		advancedButton = new Button("Show advanced");
		advancedButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent clickEvent) {
				try {
					form.showAdvanced(!form.isAdvancedVisible());
					if (form.isAdvancedVisible()) {
						advancedButton.setHTML("Show basic");
					} else {
						advancedButton.setHTML("Show advanced");
					}
					submitButton.setVisible(!form.isAdvancedVisible());
				} catch (XMLException ex) {
					// should not happen..
				}
			}
		});
		panel.add(advancedButton);

		submitButton = new Button("Submit");
		submitButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				submit();
			}
		});
		panel.add(submitButton);

		dialog.setWidget(panel);
		dialog.center();
		
		sendRetrieveForm();
	}
	
	private void sendRetrieveForm() {
		try {
			JabberDataElement data = form.getData();
			factory.devices().executeDeviceHostAdHocCommand(deviceHostJid, "add-device", Action.execute, data, new AdHocCommansModule.AdHocCommansAsyncCallback() {
			@Override
			protected void onResponseReceived(String sessionid, String node, State status, JabberDataElement data) throws JaxmppException {
				if (status == State.completed) {
					dialog.hide();
					Timer timer = new Timer() {
						@Override
						public void run() {
							try {
								factory.devices().refreshDevices();						
							} catch (JaxmppException ex) {
								Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
							}
						}
					};
					timer.schedule(300);
				} else {
					form.setData(data);
					advancedButton.setVisible(form.hasAdvanced());
				}
			}

			@Override
			public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
				String errorText = error.getElementName();
				List<Element> errorTexts = responseStanza.getFirstChild("error").getChildren("text");
				if (!errorTexts.isEmpty()) {
					errorText = errorTexts.get(0).getValue();
					for (int i=1; i<errorTexts.size(); i++) {
						errorText += "\n" + errorTexts.get(i).getValue();
					}					
				}
				new MessageDialog("Error", errorText).show();
			}

			@Override
			public void onTimeout() throws JaxmppException {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
			}
		});
		} catch (JaxmppException ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void submit() {
		sendRetrieveForm();
	}
}
