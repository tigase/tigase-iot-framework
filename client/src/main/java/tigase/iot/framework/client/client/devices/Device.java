/*
 * Device.java
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
	protected final Label header;
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
