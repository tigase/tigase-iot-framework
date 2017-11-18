/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.iot.framework.client.client.ClientFactory;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.iot.framework.client.values.Light;

/**
 * Class is an implementation of a representation (UI) for a remote light dimmer device.
 * @author andrzej
 */
public class LightsDimmer extends DeviceRemoteConfigAware<Integer, Light, tigase.iot.framework.client.devices.LightDimmer> {
		
	public LightsDimmer(ClientFactory factory, tigase.iot.framework.client.devices.LightDimmer sensor) {
		super(factory, "lights-dimmer", "\uD83D\uDCA1", sensor);
	}
	
	@Override
	public void setValue(Integer value) {
		String str = value == null ? "--" : String.valueOf(value);
		setValue(str + " %");
	}

	@Override
	protected FlowPanel prepareContextMenu(DialogBox dialog) {
		FlowPanel panel =  super.prepareContextMenu(dialog);
		
		Label changeValue = new Label("Adjust");
		changeValue.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				dialog.hide();
				showValues();
			}
		});
		
		panel.insert(changeValue, 0);
		
		return panel;
	}
	
	protected void showValues() {
		final DialogBox dialog = new DialogBox(true, true);
		dialog.setStylePrimaryName("dialog-window");

		FlowPanel panel = new FlowPanel();
		panel.setStylePrimaryName("context-menu");
		for (int i=10; i>=0; i--) {
			final int value = i * 10;
			String label = "" + value + "%";
			if (value < 50) {
				label += " \uD83D\uDD05";
			}
			if (value >= 50) {
				label += " \uD83D\uDD06";
			}
			Label item = new Label(label);
			item.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
			item.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					try {
						device.setValue(new Light(value, Light.Unit.procent), new tigase.iot.framework.client.Device.Callback<Light>() {
							@Override
							public void onError(XMPPException.ErrorCondition error) {
								Window.alert("failed to change light level = " + error);
							}
							
							@Override
							public void onSuccess(Light result) {
								LightsDimmer.this.valueChanged(result);
								dialog.hide();
							}
						});
					} catch (JaxmppException ex) {
						Logger.getLogger(LightsDimmer.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			});
			panel.add(item);
		}
		dialog.setWidget(panel);
		dialog.center();
	}	
	
}
