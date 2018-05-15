package tigase.iot.framework.client.client.devices;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.values.OnOffState;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AbstractSwitch extends DeviceRemoteConfigAware<Boolean, OnOffState, tigase.iot.framework.client.devices.AbstractSwitch> {

	private boolean state;

	public AbstractSwitch(ClientFactory factory, String deviceClass, ImageResource icon, tigase.iot.framework.client.devices.AbstractSwitch sensor) {
		super(factory, deviceClass, icon, sensor);
		setValue(false);
	}

	@Override
	public void setValue(Boolean value) {
		if (value == null) {
			setValue("--");
		} else {
			this.state = value;
			setValue(value ? "ON" : "OFF");
		}
	}

	@Override
	protected FlowPanel prepareContextMenu(DialogBox dialog) {
		FlowPanel panel =  super.prepareContextMenu(dialog);
		Label changeValue = new Label(state ? "Disable" : "Enable");
		changeValue.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				try {
					device.setValue(new OnOffState(!state), new tigase.iot.framework.client.Device.Callback<OnOffState>() {
						@Override
						public void onError(XMPPException.ErrorCondition error) {
							Window.alert("failed to change switch state = " + error);
						}

						@Override
						public void onSuccess(OnOffState result) {
							AbstractSwitch.this.valueChanged(result);
							dialog.hide();
						}
					});
				} catch (JaxmppException ex) {
					Logger.getLogger(Switch.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});

		panel.insert(changeValue, 0);

		return panel;
	}

}
