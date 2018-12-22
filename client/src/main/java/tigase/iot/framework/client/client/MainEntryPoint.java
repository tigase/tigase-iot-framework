/*
 * MainEntryPoint.java
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

package tigase.iot.framework.client.client;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.http.client.URL;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.web.bindery.event.shared.EventBus;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule;
import tigase.jaxmpp.core.client.xmpp.modules.streammng.StreamManagementModule;
import tigase.jaxmpp.gwt.client.GwtSessionObject;
import tigase.jaxmpp.gwt.client.Jaxmpp;
import tigase.jaxmpp.gwt.client.connectors.BoshConnector;
import tigase.iot.framework.client.client.auth.AuthPlace;
import tigase.iot.framework.client.client.auth.AuthEvent;
import tigase.iot.framework.client.client.auth.AuthRequestEvent;
import tigase.iot.framework.client.client.devices.DevicesListPlace;

/**
 * Main entry point.
 */
public class MainEntryPoint implements EntryPoint {

	private ClientFactory factory;

	private SimpleLayoutPanel rootPanel = new SimpleLayoutPanel();
	/**
	 * The entry point method, called automatically by loading a module that
	 * declares an implementing class as an entry-point
	 */
	@Override
	public void onModuleLoad() {
		factory = GWT.create(ClientFactory.class);

		EventBus eventBus = factory.eventBus();
		final PlaceController placeController = factory.placeController();

		// Start ActivityManager for the main widget with our ActivityMapper
		ActivityMapper activityMapper = new AppActivityMapper(factory);
		ActivityManager activityManager = new ActivityManager(activityMapper, eventBus);
		activityManager.setDisplay(rootPanel);

		Place defaultPlace = new AuthPlace();

		// Start PlaceHistoryHandler with our PlaceHistoryMapper
		AppPlaceHistoryMapper historyMapper = GWT.create(AppPlaceHistoryMapper.class);
		PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);
		historyHandler.register(placeController, eventBus, defaultPlace);

		RootLayoutPanel.get().add(rootPanel);
		
		eventBus.addHandler(AuthEvent.TYPE, new AuthEvent.Handler() {
			@Override
			public void authenticated(JID jid) {
				if (factory.jaxmpp().getSessionObject().getProperty("BINDED_RESOURCE_JID") != null) {
					placeController.goTo(new DevicesListPlace());
					//Window.alert("Authenticated as " + jid.toString());
				} else {
					placeController.goTo(new AuthPlace());
				}
			}

			@Override
			public void deauthenticated(String msg, SaslModule.SaslError saslError) {
				placeController.goTo(new AuthPlace());
			}
		});
		eventBus.addHandler(AuthRequestEvent.TYPE, new AuthRequestEvent.Handler() {
			@Override
			public void authenticate(JID jid, String password, String boshUrl) {
				factory.jaxmpp().getConnectionConfiguration().setUserJID(jid != null ? jid.getBareJid() : null);
				factory.jaxmpp().getConnectionConfiguration().setUserPassword(password);
				factory.jaxmpp().getConnectionConfiguration().setBoshService(boshUrl);
				if (jid == null) {
					int i1 = boshUrl.indexOf("//") + 2;
					int i2 = boshUrl.indexOf(":", i1);
					if (i2 == -1) {
						i2 = boshUrl.indexOf("/", i1);
					}
					if (i2 == -1) {
						i2 = boshUrl.length();
					}
					factory.jaxmpp().getConnectionConfiguration().setDomain(boshUrl.substring(i1, i2));
				}
				
				try {
					factory.jaxmpp().login();
				} catch (JaxmppException ex) {
					Logger.getLogger(MainEntryPoint.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});

		
		placeController.goTo(new AuthPlace());
		
//		FlexGrid flexGrid = new FlexGrid();
//
//		for (int i = 0; i < 20; i++) {
//			int x = i % 3;
//			if (x == 0) {
//				Thermometer item = new Thermometer("Temperature sensor", new Double(i));
//				flexGrid.add(item);
//			} else if (x == 1) {
//				LightSensor item = new LightSensor("Light sensor", new Double(i * 10));
//				flexGrid.add(item);
//			} else if (x == 2) {
//				LightsDimmer item = new LightsDimmer("Lights dimmer", i * 5);
//				flexGrid.add(item);
//			}
//		}
//
//		RootPanel.get().add(flexGrid);
	}

}
