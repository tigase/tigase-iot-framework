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
		String type = null;
		if (request.getForm() != null) {
			Field<String> typeField = ((Field<String>) request.getForm().getField("type"));
			if (typeField != null) {
				type = typeField.getFieldValue();
			}
		}

		if (type == null) {
			response.setForm(new JabberDataElement(XDataType.form));
			ListSingleField typeField = response.getForm().addListSingleField("type", null);
			typeField.setLabel("Device type");
			for (DeviceManager.DeviceTypeInfo deviceTypeInfo : deviceManager.getKnownDeviceTypes()) {
				typeField.addOption(deviceTypeInfo.getName(), deviceTypeInfo.getId());
			}
			response.setState(State.executing);
		} else {
			if (request.getForm().getFields().size() == 1) {
				JabberDataElement form = deviceManager.getDeviceForm(type);
				if (form != null && form.getFields().size() > 0) {
					form.addHiddenField("type", type);
					response.setForm(form);
					response.setState(State.executing);
				} else {
					response.setState(State.completed);
				}
			} else {
				try {
					deviceManager.createDevice(type, request.getForm());
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
