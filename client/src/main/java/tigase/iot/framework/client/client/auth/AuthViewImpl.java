/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.auth;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.*;
import tigase.iot.framework.client.client.ClientFactory;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule;

/**
 *
 * @author andrzej
 */
public class AuthViewImpl extends Composite implements AuthView {

	private final ClientFactory factory;

	private TextBox username;
	private PasswordTextBox password;
	private AbsolutePanel errorPanel;
	private Button authButton;
	private Button registerButton;
	private CheckBox remoteConnection;


	public AuthViewImpl(ClientFactory factory) {
		this.factory = factory;
		this.factory.eventBus().addHandler(AuthEvent.TYPE, new AuthEvent.Handler() {

			@Override
			public void authenticated(JID jid) {
				authFinished();
			}

			@Override
			public void deauthenticated(String msg, SaslModule.SaslError saslError) {
				authFinished();
			}

		});
		
		AbsolutePanel panel = new AbsolutePanel();
		panel.setStyleName("auth");
		Panel authPanel = createAuthPanel();
		panel.add(authPanel);
		initWidget(panel);
				
	}

	@Override
	public void refresh() {
		Storage store = Storage.getLocalStorageIfSupported();
		if (store != null) {
			username.setText(store.getItem("username"));
		}
	}
	
	protected Panel createAuthPanel() {
		FlowPanel panel = new FlowPanel();

		Label header = new Label("Authenticate");
		header.setStyleName("auth-header");
		panel.add(header);

		Label jidLabel = new Label("Username");
		jidLabel.setStyleName("auth-label");
		panel.add(jidLabel);

		username = new TextBox();
		username.setStyleName("auth-text-box");
		panel.add(username);

		Label passwordLabel = new Label("Password");
		passwordLabel.setStyleName("auth-label");
		panel.add(passwordLabel);

		password = new PasswordTextBox();
		password.setStyleName("auth-text-box");
		panel.add(password);

		if ("true".equals(Dictionary.getDictionary("config").get("allowRemoteLogin"))) {
			remoteConnection = new CheckBox("Connect to remote hub");
			remoteConnection.setStyleName("auth-checkbox");
			remoteConnection.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
				@Override
				public void onValueChange(ValueChangeEvent<Boolean> event) {
					registerButton.setEnabled(!event.getValue());
				}
			});
			panel.add(remoteConnection);
		}
		
		errorPanel = new AbsolutePanel();
		errorPanel.setStyleName("auth-errors");
		errorPanel.setVisible(false);
		panel.add(errorPanel);

		factory.eventBus().addHandler(AuthEvent.TYPE, new AuthEvent.Handler() {
			@Override
			public void authenticated(JID jid) {
				errorPanel.setVisible(false);
			}

			@Override
			public void deauthenticated(String msg, SaslModule.SaslError saslError) {
				if (msg != null) {
					errorPanel.getElement().setInnerText(msg);
				}
				if (saslError != null) {
					errorPanel.getElement().setInnerText("Authentication error: " + saslError.name());
				}
				if (msg != null || saslError != null) {
					errorPanel.setVisible(true);
				}
			}
		});

		authButton = new Button("Authenticate");
		authButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				handle();
			}
		});
		authButton.setStyleName("auth-button");
		panel.add(authButton);

		registerButton = new Button("Register");
		registerButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				new RegisterDlg(factory).show();
			}
		});
		registerButton.setStyleName("register-button");
		panel.add(registerButton);
		
		//panel.setStylePrimaryName("auth");

		KeyUpHandler handler = new KeyUpHandler() {

			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
					authButton.click();
				}
			}

		};

		username.addKeyUpHandler(handler);
		password.addKeyUpHandler(handler);

		return panel;
	}
	
	private void authFinished() {
		authButton.setEnabled(true);
	}

	private boolean isRemote() {
		return remoteConnection != null && remoteConnection.getValue();
	}
	
	private void handle() {
		errorPanel.setVisible(false);
		authButton.setEnabled(false);

		factory.devices().setRemoteMode(isRemote());

		String url;
		String domain;
		String enteredUserName = username.getText();
		if (enteredUserName != null && enteredUserName.contains("@")) {
			BareJID jid = BareJID.bareJIDInstance(enteredUserName);
			domain = jid.getDomain();
			url = "ws://web." + domain + ":5290/";
			enteredUserName = jid.getLocalpart();
		} else if (isRemote()) {
			url = "ws://web.iot1.cloud:5290/";
			domain = "iot1.cloud";
		} else {
			url = "ws://tigase-iot-hub.local:5290/";
			domain = "tigase-iot-hub.local";
		}

		Storage store = Storage.getLocalStorageIfSupported();
		if (store != null) {
			store.setItem("username", enteredUserName);
		}

		if (enteredUserName == null || enteredUserName.isEmpty()) {
			factory.eventBus().fireEvent(new AuthRequestEvent(null, null, url));
		} else {
			factory.eventBus()
					.fireEvent(new AuthRequestEvent(JID.jidInstance(enteredUserName, domain), password.getText(), url));
		}
	}
}
