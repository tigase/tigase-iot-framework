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
