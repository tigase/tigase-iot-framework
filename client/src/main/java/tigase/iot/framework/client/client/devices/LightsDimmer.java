/*
 * LightsDimmer.java
 *
 * Tigase IoT Framework
 * Copyright (C) 2011-2018 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.values.Light;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class is an implementation of a representation (UI) for a remote light dimmer device.
 *
 * @author andrzej
 */
public class LightsDimmer
		extends DeviceRemoteConfigAware<Integer, Light, tigase.iot.framework.client.devices.LightDimmer> {

	public LightsDimmer(ClientFactory factory, tigase.iot.framework.client.devices.LightDimmer sensor) {
		super(factory, "lights-dimmer", Icons.INSTANCE.dimmer(), sensor);
	}

	@Override
	protected FlowPanel prepareContextMenu(DialogBox dialog) {
		FlowPanel panel = super.prepareContextMenu(dialog);

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

	@Override
	public void setValue(Integer value) {
		String str = value == null ? "--" : String.valueOf(value);
		setValue(str + " %");
	}

	protected void showValues() {
		final DialogBox dialog = new DialogBox(true, true);
		dialog.setStylePrimaryName("dialog-window");

		FlowPanel panel = new FlowPanel();
		panel.setStylePrimaryName("context-menu");
		for (int i = 10; i >= 0; i--) {
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
						device.setValue(new Light(value, Light.Unit.procent),
										new tigase.iot.framework.client.Device.Callback<Light>() {
											@Override
											public void onError(Stanza response,
																XMPPException.ErrorCondition errorCondition) {
												Window.alert("failed to change light level = " + response);
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
