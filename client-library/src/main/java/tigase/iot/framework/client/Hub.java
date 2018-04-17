/*
 * Hub.java
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
package tigase.iot.framework.client;

import tigase.iot.framework.client.modules.SubscriptionModule;
import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xmpp.forms.Field;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.forms.XDataType;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.Action;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.AdHocCommansModule;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.State;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;

import java.util.ArrayList;
import java.util.List;

public class Hub implements JaxmppCore.LoggedInHandler, SubscriptionModule.SubscriptionChangedHandler {

	private final JaxmppCore jaxmpp;
	private final Devices devices;
	private SubscriptionModule.Subscription subscription;

	public Hub(JaxmppCore jaxmpp, Devices devices) {
		this.jaxmpp = jaxmpp;
		this.devices = devices;
		this.jaxmpp.getModulesManager().register(new SubscriptionModule());
		this.jaxmpp.getEventBus().addHandler(JaxmppCore.LoggedInHandler.LoggedInEvent.class, this);
		this.jaxmpp.getEventBus().addHandler(SubscriptionModule.SubscriptionChangedHandler.SubscriptionChangedEvent.class, this);
	}
	
	public void getRemoteConnectionCredentials(final RemoteConnectionCredentialsCallback callback)
			throws JaxmppException {
		JID jid = JID.jidInstance("pubsub." + jaxmpp.getSessionObject().getUserBareJid().getDomain());
		jaxmpp.getModule(AdHocCommansModule.class)
				.execute(jid, "get-remote-credentials", Action.execute, null,
						 new AdHocCommansModule.AdHocCommansAsyncCallback() {
							 @Override
							 protected void onResponseReceived(String sessionid, String node, State status,
															   JabberDataElement data) throws JaxmppException {
								 String username = ((Field<String>) data.getField("username")).getFieldValue();
								 String password = ((Field<String>) data.getField("password")).getFieldValue();
								 String domain = ((Field<String>) data.getField("domain")).getFieldValue();

								 callback.onResult(username, password, domain, null);
							 }

							 @Override
							 public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
									 throws JaxmppException {
								 callback.onResult(null, null, null, error);
							 }

							 @Override
							 public void onTimeout() throws JaxmppException {
								 callback.onResult(null, null, null, XMPPException.ErrorCondition.remote_server_timeout);
							 }
						 });
	}

	public void checkRemoteConnectionStatus(final RemoteConnectionStatusCallback callback) throws JaxmppException {
		JID jid = JID.jidInstance("pubsub." + jaxmpp.getSessionObject().getUserBareJid().getDomain());
		jaxmpp.getModule(AdHocCommansModule.class)
				.execute(jid, "check-remote-connection-status", Action.execute, null,
						 new AdHocCommansModule.AdHocCommansAsyncCallback() {
							 @Override
							 protected void onResponseReceived(String sessionid, String node, State status,
															   JabberDataElement data) throws JaxmppException {
								 RemoteConnectionStatusCallback.State state = RemoteConnectionStatusCallback.State.valueOf(
										 ((Field<String>) data.getField("state")).getFieldValue());
								 Integer retry = Integer.parseInt(
										 ((Field<String>) data.getField("retry")).getFieldValue());

								 callback.onResult(state, retry, null);
							 }

							 @Override
							 public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
									 throws JaxmppException {
								 callback.onResult(null, null, error);
							 }

							 @Override
							 public void onTimeout() throws JaxmppException {
								 callback.onResult(null, null, XMPPException.ErrorCondition.remote_server_timeout);
							 }
						 });
	}

	public void forceRemoteConnectionReconnection(final RemoteConnectionReconnectionCallback callback)
			throws JaxmppException {
		JID jid = JID.jidInstance("pubsub." + jaxmpp.getSessionObject().getUserBareJid().getDomain());
		jaxmpp.getModule(AdHocCommansModule.class)
				.execute(jid, "force-reconnection-to-remote-hub", Action.execute, null,
						 new AdHocCommansModule.AdHocCommansAsyncCallback() {
							 @Override
							 protected void onResponseReceived(String sessionid, String node, State status,
															   JabberDataElement data) throws JaxmppException {
								 callback.onResult(null);
							 }

							 @Override
							 public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
									 throws JaxmppException {
								 callback.onResult(error);
							 }

							 @Override
							 public void onTimeout() throws JaxmppException {
								 callback.onResult(XMPPException.ErrorCondition.remote_server_timeout);
							 }
						 });
	}

	public SubscriptionModule.Subscription getSubscription() {
		 return subscription;
	}

	private void executeManageAccountsAction(JabberDataElement data, AsyncCallback callback) throws JaxmppException {
		//JID jid = devices.isRemoteMode() ? : JID.jidInstance(jaxmpp.getSessionObject().getUserBareJid().getDomain());
		devices.executeDeviceHostAdHocCommand(JID.jidInstance(jaxmpp.getSessionObject().getUserBareJid().getDomain()),
											  "manage-accounts", Action.execute, data, callback);
	}

	public void retrieveAccounts(final RetrieveAccountsCallback callback) throws JaxmppException {
		JabberDataElement data = new JabberDataElement(XDataType.submit);
		data.addListSingleField("action", "list");
		executeManageAccountsAction(data, new AdHocCommansModule.AdHocCommansAsyncCallback() {

			@Override
			public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) throws JaxmppException {
				callback.onResult(null, error);
			}

			@Override
			public void onTimeout() throws JaxmppException {
				callback.onResult(null, XMPPException.ErrorCondition.remote_server_timeout);
			}

			@Override
			protected void onResponseReceived(String sessionid, String node, State cmdStatus, JabberDataElement data)
					throws JaxmppException {
				List<RetrieveAccountsCallback.Result> accounts = new ArrayList<>();
				for (Element item : data.getChildren()) {
					if (!"item".equals(item.getName())) {
						continue;
					}

					JID jid = null;
					RetrieveAccountsCallback.Result.Status status = null;

					for (Element field : item.getChildren()) {
						if (!"field".equals(field.getName())) {
							continue;
						}
						if ("JID".equals(field.getAttribute("var"))) {
							jid = JID.jidInstance(field.getFirstChild("value").getValue());
						}
						if ("Status".equals(field.getAttribute("var"))) {
							status = RetrieveAccountsCallback.Result.Status.valueOf(field.getFirstChild("value").getValue());
						}
					}

					accounts.add(new RetrieveAccountsCallback.Result(Hub.this, jid, status));
				}
				callback.onResult(accounts, null);
			}
		});
	}

	@Override
	public void onLoggedIn(SessionObject sessionObject) {
		try {
			this.jaxmpp.getModulesManager().getModule(SubscriptionModule.class).retrieveSubscription(null);
		} catch (JaxmppException ex) {
			// ignoring...
		}
	}

	@Override
	public void subscriptionChanged(SessionObject sessionObject, SubscriptionModule.Subscription subscription) {
		this.subscription = subscription;
	}

	public interface RemoteConnectionCredentialsCallback {

		void onResult(String username, String password, String domain, XMPPException.ErrorCondition errorCondition);

	}

	public interface RemoteConnectionStatusCallback {

		void onResult(State state, Integer retry, XMPPException.ErrorCondition errorCondition);

		enum State {
			awaitReconnection,
			reconnecting,
			connected
		}

	}

	public interface RemoteConnectionReconnectionCallback {

		void onResult(XMPPException.ErrorCondition errorCondition);

	}

	public interface RetrieveAccountsCallback {

		void onResult(List<Result> results, XMPPException.ErrorCondition error);

		class Result {

			private final Hub hub;
			public final JID jid;
			public final Status status;

			public Result(Hub hub, JID jid, Status status) {
				this.hub = hub;
				this.jid = jid;
				this.status = status;
			}

			public void enable(ActionCallback actionCallback) {
				executeAction("enable", actionCallback);
			}

			public void disable(ActionCallback actionCallback) {
				executeAction("disable", actionCallback);
			}

			public void delete(ActionCallback actionCallback) {
				executeAction("delete", actionCallback);
			}

			private void executeAction(String action, final ActionCallback callback) {
				try {
					JabberDataElement data = new JabberDataElement(XDataType.submit);
					data.addListSingleField("action", action);
					data.addJidSingleField("jid", this.jid);

					hub.executeManageAccountsAction(data, new AdHocCommansModule.AdHocCommansAsyncCallback() {

								@Override
								public void onError(Stanza responseStanza, XMPPException.ErrorCondition error)
										throws JaxmppException {
									Element errorEl = responseStanza.getFirstChild("error");
									String msg = null;
									if (errorEl != null) {
										Element text = errorEl.getFirstChild("text");
										if (text != null) {
											msg = text.getValue();
										}
									}
									callback.onError(error, msg);
								}

								@Override
								public void onTimeout() throws JaxmppException {
									callback.onError(XMPPException.ErrorCondition.remote_server_timeout,
													 "Operation timed out.");
								}

								@Override
								protected void onResponseReceived(String sessionid, String node, State status,
																  JabberDataElement data) throws JaxmppException {
									callback.onSuccess();
								}
							});
				} catch (JaxmppException ex) {
					callback.onError(XMPPException.ErrorCondition.undefined_condition, ex.getMessage());
				}
			}

			public enum Status {
				active,
				disabled,
				pending
			}

			public interface ActionCallback {

				void onError(XMPPException.ErrorCondition errorCondition, String errorMessage);

				void onSuccess();

			}
		}

	}
}
