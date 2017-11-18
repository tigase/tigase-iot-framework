/*
 * RemoveDeviceCommand.java
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

import tigase.iot.framework.runtime.DeviceManager;
import tigase.jaxmpp.core.client.JID;
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

import java.util.Comparator;
import java.util.Map;

/**
 * Command responsible for selection of a device for removal and actual removal of device.
 */
public class RemoveDeviceCommand implements AdHocCommand {

	@Inject
	private DeviceManager deviceManager;

	@Override
	public String[] getFeatures() {
		return new String[0];
	}

	@Override
	public String getName() {
		return "Remove device";
	}

	@Override
	public String getNode() {
		return "remove-device";
	}

	@Override
	public void handle(AdHocRequest request, AdHocResponse response) throws JaxmppException {
		String deviceId = null;
		if (request.getForm() != null) {
			deviceId = ((Field<String>) request.getForm().getField("device")).getFieldValue();
		}
		if (deviceId == null) {
			response.setForm(new JabberDataElement(XDataType.form));
			ListSingleField deviceField = response.getForm().addListSingleField("device", null);
			Map<String, String> devices = deviceManager.getDevices();
			devices.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).forEach(e -> {
				try {
					deviceField.addOption(e.getValue(), e.getKey());
				} catch (Exception ex) {
					// impossible!!
				}
			});
			response.setState(State.executing);
		} else {
			deviceManager.removeDevice(deviceId);
			response.setState(State.completed);
		}
	}

	@Override
	public boolean isAllowed(JID jid) {
		return true;
	}
}
