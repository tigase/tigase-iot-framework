/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.rpi.home.client.client.auth;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import tigase.rpi.home.client.client.ClientFactory;

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
