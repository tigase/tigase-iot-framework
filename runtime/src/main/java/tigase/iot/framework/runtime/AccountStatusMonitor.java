/*
 * AccountStatusMonitor.java
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
package tigase.iot.framework.runtime;

import tigase.bot.Autostart;
import tigase.bot.RequiredXmppModules;
import tigase.bot.runtime.AbstractXmppBridge;
import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.criteria.ElementCriteria;
import tigase.jaxmpp.core.client.eventbus.EventHandler;
import tigase.jaxmpp.core.client.eventbus.JaxmppEvent;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ContextAware;
import tigase.jaxmpp.j2se.Jaxmpp;

@Autostart
@RequiredXmppModules({AccountStatusMonitor.AccountStatusMonitorModule.class})
public class AccountStatusMonitor extends AbstractXmppBridge {

	@Override
	protected void jaxmppConnected(Jaxmpp jaxmpp) {
		
	}

	@Override
	protected void jaxmppDisconnected(Jaxmpp jaxmpp) {

	}

	public static class AccountStatusMonitorModule implements XmppModule, ContextAware {

		public static final String ACCOUNT_STATUS_KEY = "account-status-key";

		private static final Criteria CRITERIA = ElementCriteria.name("iq", "jabber:client").add(ElementCriteria.name("account", "http://tigase.org/protocol/iot-account"));

		private Context context;

		@Override
		public Criteria getCriteria() {
			return CRITERIA;
		}

		@Override
		public String[] getFeatures() {
			return new String[0];
		}

		@Override
		public void process(Element element) throws XMPPException, XMLException, JaxmppException {
			Element account = element.getChildrenNS("account", "http://tigase.org/protocol/iot-account");
			AccountStatus status = AccountStatus.valueOf(account.getAttribute("status"));
			context.getSessionObject().setProperty(ACCOUNT_STATUS_KEY, status);
			context.getEventBus().fire(new AccountStatusChangedHandler.AccountStatusChangedEvent(context.getSessionObject(), status));
			
			switch (status) {
				case active:
					context.getEventBus().fire(new JaxmppCore.LoggedInHandler.LoggedInEvent(context.getSessionObject()));
				case pending:
				case disabled:
					break;
			}
		}

		@Override
		public void setContext(Context context) {
			this.context = context;
		}
	}

	public enum AccountStatus {
		active,
		pending,
		disabled
	}

	public interface AccountStatusChangedHandler
			extends EventHandler {

		public class AccountStatusChangedEvent extends JaxmppEvent<AccountStatusChangedHandler> {

			protected final AccountStatus accountStatus;

			/**
			 * Constructs event object.
			 *
			 * @param sessionObject session object.
			 */
			protected AccountStatusChangedEvent(SessionObject sessionObject, AccountStatus accountStatus) {
				super(sessionObject);
				this.accountStatus = accountStatus;
			}

			@Override
			public void dispatch(AccountStatusChangedHandler handler) throws Exception {
				handler.accountStateChanged(sessionObject, accountStatus);
			}

			public AccountStatus getAccountStatus() {
				return accountStatus;
			}

		}

		void accountStateChanged(SessionObject sessionObject, AccountStatus accountStatus);
	}

}
