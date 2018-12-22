/*
 * ClientFactoryImpl.java
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
package tigase.iot.framework.client.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import tigase.iot.framework.client.Devices;
import tigase.iot.framework.client.Hosts;
import tigase.iot.framework.client.Hub;
import tigase.iot.framework.client.client.account.AccountLockedPlace;
import tigase.iot.framework.client.client.account.AccountLockedView;
import tigase.iot.framework.client.client.account.AccountLockedViewImpl;
import tigase.iot.framework.client.client.auth.AuthEvent;
import tigase.iot.framework.client.client.auth.AuthView;
import tigase.iot.framework.client.client.auth.AuthViewImpl;
import tigase.iot.framework.client.client.devices.DevicesListPlace;
import tigase.iot.framework.client.client.devices.DevicesListView;
import tigase.iot.framework.client.client.devices.DevicesListViewImpl;
import tigase.iot.framework.client.client.ui.MessageDialog;
import tigase.iot.framework.client.modules.AccountStatusModule;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.AdHocCommansModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule.Identity;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.core.client.xmpp.modules.streammng.StreamManagementModule;
import tigase.jaxmpp.core.client.xmpp.utils.DateTimeFormat;
import tigase.jaxmpp.core.client.xmpp.utils.DateTimeFormat.DateTimeFormatProvider;
import tigase.jaxmpp.gwt.client.Jaxmpp;
import tigase.jaxmpp.gwt.client.Presence;
import tigase.jaxmpp.gwt.client.Roster;

import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author andrzej
 */
public class ClientFactoryImpl implements ClientFactory {

	private static final Logger log = Logger.getLogger(ClientFactoryImpl.class.getCanonicalName());

	private final Jaxmpp jaxmpp = new Jaxmpp();
	private final Devices devices;

	private final EventBus eventBus = GWT.create(SimpleEventBus.class);
	private final AccountLockedView accountLockedView;
	private final AuthViewImpl authView;
	private final DevicesListViewImpl devicesListView;
	private final PlaceController placeController;
	private final Hosts hosts;
	private final Hub hub;
	
//	private final ResourceBindHandler jaxmppBindListener = new ResourceBindHandler();

	public ClientFactoryImpl() {
		DateTimeFormat.setProvider(new DateTimeFormatProvider() {

			private final TimeZone utcZone = TimeZone.createTimeZone(0);
			
			private final com.google.gwt.i18n.client.DateTimeFormat df1 = com.google.gwt.i18n.client.DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			private final com.google.gwt.i18n.client.DateTimeFormat df2 = com.google.gwt.i18n.client.DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			private final com.google.gwt.i18n.client.DateTimeFormat df3 = com.google.gwt.i18n.client.DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			private final com.google.gwt.i18n.client.DateTimeFormat df4 = com.google.gwt.i18n.client.DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

			@Override
			public String format(Date date) {
				return df4.format(date, utcZone);
			}

			@Override
			public Date parse(String t) {
				try {
					return df1.parse(t);
				} catch (Exception e) {
					try {
						return df2.parse(t);
					} catch (Exception e1) {
						try {
							return df3.parse(t);
						} catch (Exception e2) {
							return null;
						}
					}
				}
			}
		});
		
		try {
			Presence.initialize(jaxmpp());
			Roster.initialize(jaxmpp());
		} catch (JaxmppException ex) {
			log.log(Level.SEVERE, "could not initialize properly Jaxmpp instance", ex);
		}

		//jaxmpp().getModulesManager().register(new DiscoveryModule());
		jaxmpp().getModulesManager().register(new AdHocCommansModule());
		jaxmpp().getModulesManager().register(new PubSubModule());
		jaxmpp().getModulesManager().register(new CapabilitiesModule());

		devices = new Devices(jaxmpp, false);
		hosts = new Hosts(jaxmpp, devices, new Devices.DevicesInfoRetrieved() {
			@Override
			public void onDeviceInfoRetrieved(Map<JID, Identity> devicesInfo) {
				eventBus.fireEvent(new ActiveHostsChangedEvent(devicesInfo));
			}
		});
		hub = new Hub(jaxmpp, devices);

		accountLockedView = new AccountLockedViewImpl(this);
		authView = new AuthViewImpl(this);
		devicesListView = new DevicesListViewImpl(this);
		placeController = new PlaceController(eventBus());

		jaxmpp().getEventBus().addHandler(JaxmppCore.LoggedInHandler.LoggedInEvent.class, new JaxmppCore.LoggedInHandler() {
			@Override
			public void onLoggedIn(SessionObject sessionObject) {
				JID jid = ResourceBinderModule.getBindedJID(sessionObject);
				eventBus().fireEvent(new AuthEvent(jid));
			}
		});
		
		jaxmpp().getEventBus().addHandler(JaxmppCore.LoggedOutHandler.LoggedOutEvent.class, new JaxmppCore.LoggedOutHandler() {
			@Override
			public void onLoggedOut(SessionObject sessionObject) {
				if (StreamManagementModule.isResumptionEnabled(jaxmpp().getSessionObject())) {
					Logger.getLogger(ClientFactoryImpl.class.getName()).severe("trying to resume broken connection");
					try {
						jaxmpp().login();
					} catch (JaxmppException ex) {
						Logger.getLogger(ClientFactoryImpl.class.getName()).log(Level.SEVERE, null, ex);
					}
				} else {
					eventBus().fireEvent(new AuthEvent(null));
				}
			}
		});

		jaxmpp().getEventBus().addHandler(AuthModule.AuthFailedHandler.AuthFailedEvent.class, new AuthModule.AuthFailedHandler() {

			@Override
			public void onAuthFailed(SessionObject sessionObject, SaslModule.SaslError error) throws JaxmppException {
				eventBus().fireEvent(new AuthEvent(null, "Authentication failed", error));
			}
		});

		jaxmpp.getEventBus().addHandler(Hub.CloudSettingsChangedHandler.CloudSettingsChangedEvent.class, new Hub.CloudSettingsChangedHandler() {
			@Override
			public void onCloudSettingsChanged(SessionObject sessionObject, Hub.CloudSettings cloudSettings) {
				if (cloudSettings == null) {
					return;
				}
				if (!cloudSettings.configured) {
					new MessageDialog("Configure IoT One Cloud",
									  "Your IoT hub is not configured to connect to the IoT One Cloud. Do you wish to configure it now?",
									  new Runnable() {
										  @Override
										  public void run() {
											  new CloudActivationEmailDialog(ClientFactoryImpl.this).show();
										  }
									  }).onCancel(new Runnable() {

						@Override
						public void run() {
							try {
								hub.updateCloudSettings(false, null, new Hub.CompletionHandler() {
									@Override
									public void onResult(XMPPException.ErrorCondition errorCondition) {
										// ignoring result..
									}
								});
							} catch (JaxmppException ex) {
								// nothing to do..
							}
						}
					}).show();
				}
			}
		});

		jaxmpp.getEventBus().addHandler(AccountStatusModule.AccountStatusChangedHandler.AccountStatusChangedEvent.class,
										new AccountStatusModule.AccountStatusChangedHandler() {
											@Override
											public void accountStatusChanged(SessionObject sessionObject,
																			 Hub.RetrieveAccountsCallback.Result.Status status) {
												if (status != Hub.RetrieveAccountsCallback.Result.Status.active) {
													placeController.goTo(new AccountLockedPlace());
												} else {
													placeController.goTo(new DevicesListPlace());
													try {
														jaxmpp.getModule(PresenceModule.class).sendInitialPresence();
														devices().refreshDevices();
													} catch (Exception ex) {
														// nothing to do..
													}
												}
											}
										});

	}
	
	@Override
	public PlaceController placeController() {
		return placeController;
	}


	@Override
	public Jaxmpp jaxmpp() {
		return jaxmpp;
	}

	@Override
	public EventBus eventBus() {
		return eventBus;
	}

	@Override
	public AccountLockedView accountLockedView() {
		return accountLockedView;
	}

	@Override
	public AuthView authView() {
		return authView;
	}
	
	public Devices devices() {
		return devices;
	}
	
	@Override
	public DevicesListView devicesListView() {
		return devicesListView;
	}

	@Override
	public Hosts hosts() {
		return hosts;
	}
	
	@Override
	public Hub hub() {
		return hub;
	}
}
