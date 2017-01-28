/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.auth;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule;
import tigase.iot.framework.client.client.ClientFactory;

/**
 *
 * @author andrzej
 */
public class AuthViewImpl extends Composite implements AuthView {

	private final ClientFactory factory;

	private TextBox username;
	private PasswordTextBox password;
	private TextBox connectionUrl;
	private AbsolutePanel errorPanel;
	private Button authButton;

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
			String url = store.getItem("url");
			if (url != null) {
				connectionUrl.setText(url);
			} else {
				connectionUrl.setText(getDefConnectionUrl());
			}
		}
	}
	
	protected Panel createAuthPanel() {
		FlowPanel panel = new FlowPanel();

		Label header = new Label("Authenticate");
		header.setStyleName("auth-header");
		panel.add(header);

		Label jidLabel = new Label("XMPP ID");
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

		Label connectionUrlLabel = new Label("Connection URL");
		connectionUrlLabel.setStyleName("auth-label");
		panel.add(connectionUrlLabel);
		connectionUrl = new TextBox();
		connectionUrl.setStyleName("auth-text-box");
		connectionUrl.setText(getDefConnectionUrl());
		panel.add(connectionUrl);

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
	
	protected String getDefConnectionUrl() {
		return "ws://" + Window.Location.getHostName() + ":5290/";
	}

	private void authFinished() {
		authButton.setEnabled(true);
		authButton.addStyleName("default");
		authButton.removeStyleName("disabled");
	}

	private void handle() {
		errorPanel.setVisible(false);
		authButton.setEnabled(false);
		authButton.removeStyleName("default");
		authButton.addStyleName("disabled");
		String url = (connectionUrl != null) ? connectionUrl.getText() : null;
		if (url != null) {
			url = url.trim();
			if (url.isEmpty()) {
				url = null;
			}
		}
		
		Storage store = Storage.getLocalStorageIfSupported();
		if (store != null) {
			store.setItem("username", username.getText());
			if (url == null || url.isEmpty()) {
				store.removeItem("url");
			} else if (!getDefConnectionUrl().equals(url)) {
				store.setItem("url", url);
			}
		}
		
		if (username.getText() == null || username.getText().isEmpty()) {
			factory.eventBus().fireEvent(new AuthRequestEvent(null, null, url));
		} else {
			factory.eventBus().fireEvent(new AuthRequestEvent(JID.jidInstance(username.getText()), password.getText(), url));
		}
	}
}
