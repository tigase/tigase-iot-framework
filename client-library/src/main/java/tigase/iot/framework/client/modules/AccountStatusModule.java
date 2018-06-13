/*
 * AccountStatusModule.java
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
package tigase.iot.framework.client.modules;

import tigase.iot.framework.client.Hub;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.criteria.ElementCriteria;
import tigase.jaxmpp.core.client.eventbus.EventHandler;
import tigase.jaxmpp.core.client.eventbus.JaxmppEvent;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xmpp.modules.AbstractIQModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;

import java.util.logging.Level;

public class AccountStatusModule
		extends AbstractIQModule {

	public static final String IOT_ACCOUNT_XMLNS = "http://tigase.org/protocol/iot-account";

	private static final Criteria CRITERIA = ElementCriteria.name("iq")
			.add(ElementCriteria.name("account", IOT_ACCOUNT_XMLNS));

	public static Hub.RetrieveAccountsCallback.Result.Status getStatus(SessionObject sessionObject) {
		return sessionObject.getProperty(IOT_ACCOUNT_XMLNS + "#AccountStatus");
	}

	private static void setStatus(SessionObject sessionObject, Hub.RetrieveAccountsCallback.Result.Status status) {
		sessionObject.setProperty(IOT_ACCOUNT_XMLNS + "#AccountStatus", status);
	}

	@Override
	public Criteria getCriteria() {
		return CRITERIA;
	}

	@Override
	public String[] getFeatures() {
		return new String[0];
	}

	@Override
	protected void processGet(IQ iq) throws JaxmppException {
		throw new XMPPException(XMPPException.ErrorCondition.bad_request);
	}

	@Override
	protected void processSet(IQ iq) throws JaxmppException {
		Element accountEl = iq.getChildrenNS("account", IOT_ACCOUNT_XMLNS);
		String statusStr = accountEl.getAttribute("status");
		try {
			Hub.RetrieveAccountsCallback.Result.Status status = Hub.RetrieveAccountsCallback.Result.Status.valueOf(
					statusStr);
			setStatus(this.context.getSessionObject(), status);
			fireEvent(
					new AccountStatusChangedHandler.AccountStatusChangedEvent(this.context.getSessionObject(), status));
		} catch (Throwable ex) {
			log.log(Level.WARNING, "count not process account status change notification", ex);
		}
	}

	public interface AccountStatusChangedHandler
			extends EventHandler {

		void accountStatusChanged(SessionObject sessionObject, Hub.RetrieveAccountsCallback.Result.Status status);

		class AccountStatusChangedEvent
				extends JaxmppEvent<AccountStatusChangedHandler> {

			public final Hub.RetrieveAccountsCallback.Result.Status status;

			AccountStatusChangedEvent(SessionObject sessionObject, Hub.RetrieveAccountsCallback.Result.Status status) {
				super(sessionObject);
				this.status = status;
			}

			@Override
			public void dispatch(AccountStatusChangedHandler accountStatusChangedHandler) throws Exception {
				accountStatusChangedHandler.accountStatusChanged(sessionObject, status);
			}
		}
	}
}
