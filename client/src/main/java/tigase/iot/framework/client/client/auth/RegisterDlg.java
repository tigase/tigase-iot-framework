/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.auth;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.client.ui.Form;
import tigase.iot.framework.client.client.ui.MessageDialog;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xmpp.modules.StreamFeaturesModule;
import tigase.jaxmpp.core.client.xmpp.modules.registration.InBandRegistrationModule;
import tigase.jaxmpp.core.client.xmpp.modules.registration.UnifiedRegistrationForm;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.gwt.client.Jaxmpp;
import tigase.jaxmpp.gwt.client.connectors.BoshConnector;

/**
 *
 * @author andrzej
 */
public class RegisterDlg {

	private final ClientFactory factory;

	private final Form form;
	private final DialogBox dialog;
	private final Button okButton;

	private Jaxmpp jaxmpp;
	private RegisterHandler regHandler;

	public RegisterDlg(ClientFactory factory) {
		this.factory = factory;

		FlowPanel panel = new FlowPanel();
		panel.setStylePrimaryName("context-menu");

		form = new Form();

		dialog = new DialogBox(true, true);
		dialog.setStylePrimaryName("dialog-window");
		dialog.setTitle("Add device");

		panel.add(form);

		okButton = new Button("Submit");
		okButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				submit();
			}
		});
		panel.add(okButton);

		dialog.setWidget(panel);

		retrieveRegistrationForm();
	}

	public void show() {
		dialog.center();
	}

	private void submit() {
		try {
			jaxmpp.getModule(InBandRegistrationModule.class).register((UnifiedRegistrationForm) form.getData(), new AsyncCallback() {
				public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
					String message = null;

					if (error == null) {
						message = "Registration error";
					} else {
						switch (error) {
							case conflict:
								message = "Username not available. Choose another one.";
								break;
							default:
								message = error.name();
								break;
						}
					}

					showError(message, new Runnable() {
						public void run() {
							retrieveRegistrationForm();
						}
					});
				}

				public void onSuccess(Stanza responseStanza) throws JaxmppException {
					String message = "Registration successful";
					new MessageDialog("Success", message).show();
					enableOkButton();					
					RegisterDlg.this.dialog.hide();
					jaxmpp.disconnect();
				}

				public void onTimeout() throws JaxmppException {
					String message = "Server doesn't responses";
					showError(message, null);
				}
			});
		} catch (JaxmppException ex) {
			Logger.getLogger(RegisterDlg.class.getName()).log(Level.SEVERE, null, ex);
			enableOkButton();
		}
	}
	
	private void retrieveRegistrationForm() {
		if (jaxmpp != null && jaxmpp.isConnected()) {
			try {
				jaxmpp.disconnect();
			} catch (JaxmppException ex) {
				// ignoring exception
			}
		}
		disableOkButton();
		jaxmpp = new Jaxmpp();
		regHandler = new RegisterHandler();
		jaxmpp.getModulesManager().register(new InBandRegistrationModule());
		final InBandRegistrationModule regModule = jaxmpp.getModulesManager().getModule(InBandRegistrationModule.class);
		jaxmpp.getProperties().setUserProperty(InBandRegistrationModule.IN_BAND_REGISTRATION_MODE_KEY, Boolean.TRUE);
		jaxmpp.getProperties().setUserProperty(SessionObject.SERVER_NAME, "tigase-iot-hub.local");
		jaxmpp.getProperties().setUserProperty(BoshConnector.BOSH_SERVICE_URL_KEY, "ws://tigase-iot-hub.local:5290/");

		regModule.addNotSupportedErrorHandler(regHandler);
		regModule.addReceivedErrorHandler(regHandler);
		regModule.addReceivedRequestedFieldsHandler(regHandler);
		regModule.addReceivedTimeoutHandler(regHandler);

		jaxmpp.getModulesManager().getModule(StreamFeaturesModule.class).addStreamFeaturesReceivedHandler(new StreamFeaturesModule.StreamFeaturesReceivedHandler() {

			@Override
			public void onStreamFeaturesReceived(SessionObject sessionObject, Element featuresElement) throws JaxmppException {
				Element features = featuresElement;//ElementFactory.create(featuresElement);
				Element e = features.getChildrenNS("mechanisms", "urn:ietf:params:xml:ns:xmpp-sasl");
				if (e != null) {
					features.removeChild(e);
				}
				e = features.getChildrenNS("bind", "urn:ietf:params:xml:ns:xmpp-bind");
				if (e != null) {
					features.removeChild(e);
				}

				sessionObject.setProperty("StreamFeaturesModule#STREAM_FEATURES_ELEMENT", features);

				regModule.start();
			}
		});
		
		try {
			jaxmpp.login();
		} catch (JaxmppException ex) {
			showError(ex.getMessage(), new Runnable() {
				@Override
				public void run() {
					RegisterDlg.this.dialog.hide();
				}
			});
		}
	}
	
	private void showError(String message, Runnable runAfter) {
		new MessageDialog("Error", message, runAfter).show();
	}

	private void disableOkButton() {
		okButton.setEnabled(false);
	}
	
	private void enableOkButton() {
		okButton.setEnabled(true);
	}
	
	private class RegisterHandler implements InBandRegistrationModule.NotSupportedErrorHandler, InBandRegistrationModule.ReceivedErrorHandler,
			InBandRegistrationModule.ReceivedRequestedFieldsHandler, InBandRegistrationModule.ReceivedTimeoutHandler {

		@Override
		public void onNotSupportedError(SessionObject sessionObject) throws JaxmppException {
			showError("Registration not supported", new Runnable() {
				public void run() {
					RegisterDlg.this.dialog.hide();
				}
			});
		}

		@Override
		public void onReceivedError(SessionObject sessionObject, IQ responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
			String message = null;
			if (error == null) {
				message = "Registration error";
			} else {
				switch (error) {
					case conflict:
						message = "Username not available. Choose another one.";
						break;
					default:
						message = error.name();
						break;
				}
			}
			if (message != null) {
				showError(message, new Runnable() {
					public void run() {
						retrieveRegistrationForm();
					}
				});
			}
		}

		@Override
		public void onReceivedRequestedFields(SessionObject sessionObject, IQ responseStanza, UnifiedRegistrationForm unifiedRegistrationForm) {
			try {
				form.setData(unifiedRegistrationForm);
			} catch (JaxmppException ex) {
				Logger.getLogger(RegisterDlg.class.getName()).log(Level.SEVERE, null, ex);
			}
			enableOkButton();
		}

		@Override
		public void onReceivedTimeout(SessionObject sessionObject) throws JaxmppException {
			showError("Server doesn't respond", null);
		}
	}
	
}
