/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.iot.framework.client.Device.Configuration;
import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.client.ui.Form;

/**
 * Extended version of {@link DeviceRemoteAware} which adds support for configuration management of a remote device.
 * @author andrzej
 */
public abstract class DeviceRemoteConfigAware<S, T extends tigase.iot.framework.client.Device.IValue<S>, D extends tigase.iot.framework.client.Device<T>> extends DeviceRemoteAware<S, T>{

	protected final D device;
	
	public DeviceRemoteConfigAware(String deviceClass, String iconStr, D sensor) {
		super(deviceClass, iconStr, sensor);
		this.device = sensor;
		
		Widget w = asWidget();
		w.sinkEvents(Event.ONCLICK);
		w.addHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				clicked();
			}
		}, ClickEvent.getType());
	}

	protected void clicked() {
		final DialogBox dialog = new DialogBox(true, true);
		dialog.setStylePrimaryName("dialog-window");

		FlowPanel panel = prepareContextMenu(dialog);
		
		dialog.setWidget(panel);
		dialog.center();
	}
	
	protected FlowPanel prepareContextMenu(DialogBox dialog) {
		FlowPanel panel = new FlowPanel();
		panel.setStylePrimaryName("context-menu");
		Label rename = new Label("Change name");
		rename.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				showRenameWindow();
				dialog.hide();
			}
		});
		panel.add(rename);
		Label configure = new Label("Configure");
		configure.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				showConfigWindow();
				dialog.hide();
			}
		});
		panel.add(configure);
		
		return panel;
	}
	
	protected void showRenameWindow() {
		final DialogBox dialog = new DialogBox(true, true);
		dialog.setStylePrimaryName("dialog-window");

		FlowPanel panel = new FlowPanel();
		panel.setStylePrimaryName("context-menu");
		Label label = new Label("Rename to:");
		panel.add(label);
		TextBox input = new TextBox();
		input.setText(device.getName());
		panel.add(input);
		
		Button button = new Button("Confirm");
		button.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				String name = input.getText();
				if (name == null || name.isEmpty() || device.getName().equals(name)) {
					return;
				}
				dialog.hide();
				try {
					device.setName(name, new tigase.iot.framework.client.Device.Callback<String>() {
						@Override
						public void onError(XMPPException.ErrorCondition error) {
							Window.alert("Could not rename device:" + error);
						}

						@Override
						public void onSuccess(String name) {
							setDescription(name);
						}						
					});
				} catch (JaxmppException ex) {
					Logger.getLogger(DeviceRemoteConfigAware.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});
		panel.add(button);
		dialog.setWidget(panel);
		
		dialog.center();
	}
	
	protected void showConfigWindow() {
		try {
			device.retrieveConfiguration(new tigase.iot.framework.client.Device.Callback<Configuration>() {
				@Override
				public void onError(XMPPException.ErrorCondition error) {
					Window.alert("Failed to retrieve device configuration: " + error);
				}
				
				@Override
				public void onSuccess(Configuration result) {
					displayConfiguration(result);
				}
				
			});
		} catch (JaxmppException ex) {
			Window.alert("Failed to retrieve device configuration: " + ex.getMessage());
		}
	}
		
	public void displayConfiguration(Configuration config) {
		try {
			FlowPanel panel = new FlowPanel();
			panel.setStylePrimaryName("context-menu");
			
			Form form = new Form();			
			form.setData(config.getValue());
			
			DialogBox dialog = new DialogBox(true, true);
			dialog.setStylePrimaryName("dialog-window");
			dialog.setTitle("Configuration");
			
			panel.add(form);

			Button button = new Button("Confirm");
			button.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					try {
						JabberDataElement config = form.getData();
						dialog.hide();
					
						device.setConfiguration(config, new tigase.iot.framework.client.Device.Callback<Configuration>() {	
							public void onError(XMPPException.ErrorCondition error) {
								
							}

							public void onSuccess(Configuration result) {
								
							}
						});
					} catch (JaxmppException ex) {
						Logger.getLogger(DeviceRemoteConfigAware.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			});
			panel.add(button);

			
			dialog.setWidget(panel);
			dialog.center();
		} catch (JaxmppException ex) {
			Logger.getLogger(DeviceRemoteConfigAware.class.getName()).log(Level.SEVERE, null, ex);
		}
		
	}
}
