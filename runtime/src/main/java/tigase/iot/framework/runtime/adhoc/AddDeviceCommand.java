/*
 * AddDeviceCommand.java
 *
 * Tigase IoT Framework
 * Copyright (C) 2011-2017 "Tigase, Inc." <office@tigase.com>
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
package tigase.iot.framework.runtime.adhoc;

import tigase.iot.framework.devices.IDevice;
import tigase.iot.framework.runtime.DeviceManager;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.forms.Field;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.forms.ListSingleField;
import tigase.jaxmpp.core.client.xmpp.forms.XDataType;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.AdHocCommand;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.AdHocRequest;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.AdHocResponse;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.State;
import tigase.kernel.beans.Inject;

/**
 * Command responsible for adding a device driver with initial configuration done for which user is queried.
 */
public class AddDeviceCommand implements AdHocCommand {

	@Inject
	private DeviceManager deviceManager;

	@Override
	public String[] getFeatures() {
		return new String[0];
	}

	@Override
	public String getName() {
		return "Add device";
	}

	@Override
	public String getNode() {
		return "add-device";
	}

	@Override
	public void handle(AdHocRequest request, AdHocResponse response) throws JaxmppException {
		String category = null;
		String deviceClass = null;
		if (request.getForm() != null) {
			Field<String> categoryField = ((Field<String>) request.getForm().getField("category"));
			if (categoryField != null) {
				category = categoryField.getFieldValue();
			}
			Field<String> deviceClassField = ((Field<String>) request.getForm().getField("deviceClass"));
			if (deviceClassField != null) {
				deviceClass = deviceClassField.getFieldValue();
			}
		}

		if (category == null) {
			response.setForm(new JabberDataElement(XDataType.form));
			ListSingleField typeField = response.getForm().addListSingleField("category", null);
			typeField.setLabel("Device type");
			for (IDevice.Category deviceType : deviceManager.getKnownDeviceCategories()) {
				typeField.addOption(deviceType.getName(), deviceType.getId());
			}
			response.setState(State.executing);
		} else if (deviceClass == null) {
			JabberDataElement form = new JabberDataElement(XDataType.form);
			form.addHiddenField("category", category);
			ListSingleField deviceClassField = form.addListSingleField("deviceClass", null);
			deviceClassField.setLabel("Device");
			for (DeviceManager.DeviceDriverInfo info : deviceManager.getDeviceDriversInfo(category)) {
				deviceClassField.addOption(info.getName(), info.getId());
			}
			response.setForm(form);
			response.setState(State.executing);
		} else {
			if (request.getForm().getFields().size() == 2) {
				JabberDataElement form = deviceManager.getDeviceForm(deviceClass);
				if (form != null && form.getFields().size() > 0) {
					form.addHiddenField("category", category);
					form.addHiddenField("deviceClass", deviceClass);
					response.setForm(form);
					response.setState(State.executing);
				} else {
					response.setState(State.completed);
				}
			} else {
				try {
					deviceManager.createDevice(category, deviceClass, request.getForm());
				} catch (RuntimeException ex) {
					throw new XMPPException(XMPPException.ErrorCondition.not_acceptable, ex.getMessage(), ex);
				}
			}
		}
	}

	@Override
	public boolean isAllowed(JID jid) {
		return true;
	}
}
