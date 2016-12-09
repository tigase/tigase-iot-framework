/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.rpi.home.client.client.devices;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

/**
 *
 * @author andrzej
 */
public class Device extends Composite {

	private final Label icon;
	private final Label header;
	private final Label label;

	public Device(String deviceClass, String iconStr) {
		FlowPanel item = new FlowPanel();
		item.setStylePrimaryName("flex-device-item");
		item.addStyleName(deviceClass);

		icon = new Label(iconStr);
		icon.setStylePrimaryName("icon");

		item.add(icon);

		header = new Label();
		header.setStylePrimaryName("header");
		item.add(header);

		label = new Label();
		label.setStylePrimaryName("label");
		item.add(label);

		initWidget(item);
	}
	
	public void setDescription(String description) {
		label.setText(description);
	}
	
	protected void setValue(String value) {
		header.setText(value);
	}

}
