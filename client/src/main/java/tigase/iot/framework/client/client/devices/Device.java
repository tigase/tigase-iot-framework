/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

/**
 * Class implements a UI for a device representation.
 * @author andrzej
 */
public class Device extends Composite {

	private final Image icon;
	private final Label header;
	private final Label label;

	public Device(String deviceClass, ImageResource icon) {
		FlowPanel item = new FlowPanel();
		item.setStylePrimaryName("flex-device-item");
		item.addStyleName(deviceClass);

		this.icon = new Image(icon);
		this.icon.setStylePrimaryName("icon");

		item.add(this.icon);

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
