/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.values.OnOffState;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;

/**
 *
 * @author andrzej
 */
public class Switch extends DeviceRemoteConfigAware<Boolean, OnOffState, tigase.iot.framework.client.devices.Switch> {
	
	private boolean state;
	
	public Switch(ClientFactory factory, tigase.iot.framework.client.devices.Switch sensor) {
		super(factory, "switch", "\uD83D\uDD0C", sensor);
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
							Switch.this.valueChanged(result);
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
