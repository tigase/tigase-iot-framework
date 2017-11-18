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
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.client.ui.Form;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.forms.Field;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.Action;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.AdHocCommansModule;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.State;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

/**
 *
 * @author andrzej
 */
public class AddDeviceDlg {

	private final ClientFactory factory;
	private final JID deviceHostJid;
	private final Form form;
	private final DialogBox dialog;

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

		Button button = new Button("Submit");
		button.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				submit();
			}
		});
		panel.add(button);

		dialog.setWidget(panel);
		dialog.center();
		
		sendRetrieveForm();
	}
	
	private void sendRetrieveForm() {
		try {
			JabberDataElement data = form.getData();
			factory.jaxmpp().getModule(AdHocCommansModule.class).execute(deviceHostJid, "add-device", Action.execute, data, new AdHocCommansModule.AdHocCommansAsyncCallback() {
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
				}
			}

			@Override
			public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
