package tigase.iot.framework.client.client.account;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import tigase.iot.framework.client.client.ClientFactory;

public class AccountLockedActivity extends AbstractActivity {

		private final ClientFactory factory;

	public AccountLockedActivity(AccountLockedPlace place, ClientFactory factory_) {
			this.factory = factory_;
		}

		@Override
		public void start(AcceptsOneWidget panel, EventBus eventBus) {
			AccountLockedView view = factory.accountLockedView();
			panel.setWidget(view.asWidget());
		}
}
