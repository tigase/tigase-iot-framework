/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import tigase.iot.framework.client.client.ClientFactory;

/**
 *
 * @author andrzej
 */
public class DevicesListActivity extends AbstractActivity {

	private final ClientFactory factory;

	public DevicesListActivity(DevicesListPlace place, ClientFactory factory_) {
		this.factory = factory_;
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		panel.setWidget(factory.devicesListView().asWidget());
	}

}
