/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.i18n.client.TimeZone;
import com.google.web.bindery.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.SimpleEventBus;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule.DiscoInfoAsyncCallback;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule.Identity;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.core.client.xmpp.modules.streammng.StreamManagementModule;
import tigase.jaxmpp.core.client.xmpp.utils.DateTimeFormat;
import tigase.jaxmpp.core.client.xmpp.utils.DateTimeFormat.DateTimeFormatProvider;
import tigase.jaxmpp.gwt.client.Jaxmpp;
import tigase.jaxmpp.gwt.client.Presence;
import tigase.jaxmpp.gwt.client.Roster;
import tigase.iot.framework.client.Devices;
import tigase.iot.framework.client.client.auth.AuthEvent;
import tigase.iot.framework.client.client.auth.AuthView;
import tigase.iot.framework.client.client.auth.AuthViewImpl;
import tigase.iot.framework.client.client.devices.DevicesListView;
import tigase.iot.framework.client.client.devices.DevicesListViewImpl;

/**
 *
 * @author andrzej
 */
public class ClientFactoryImpl implements ClientFactory {

	private static final Logger log = Logger.getLogger(ClientFactoryImpl.class.getCanonicalName());

	private final Jaxmpp jaxmpp = new Jaxmpp();
	private final Devices devices;

	private final EventBus eventBus = GWT.create(SimpleEventBus.class);
	private final AuthViewImpl authView;
	private final DevicesListViewImpl devicesListView;
	private final PlaceController placeController;

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

		jaxmpp().getModulesManager().register(new PubSubModule());
		jaxmpp().getModulesManager().register(new CapabilitiesModule());

		Dictionary config = Dictionary.getDictionary("config");
		boolean pep = "true".equals(config.get("pep"));
		devices = new Devices(jaxmpp, pep);
		
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

//	private class ResourceBindHandler implements ResourceBinderModule.ResourceBindErrorHandler,
//			ResourceBinderModule.ResourceBindSuccessHandler {
//
//		@Override
//		public void onResourceBindError(SessionObject sessionObject, ErrorCondition errorCondition) {
//			MessageDialog dlg = new MessageDialog(ClientFactoryImpl.this, baseI18n().error(), errorCondition.name());
//			dlg.show();
//			dlg.center();
//			eventBus().fireEvent(new AuthEvent(null));
//		}
//
//		@Override
//		public void onResourceBindSuccess(SessionObject sessionObject, JID bindedJid) throws JaxmppException {
//			try {
//				eventBus().fireEvent(new ServerFeaturesChangedEvent(new ArrayList<Identity>(), new ArrayList<String>()));
//				jaxmpp().getModulesManager().getModule(DiscoveryModule.class).getInfo(
//						JID.jidInstance(bindedJid.getDomain()), new DiscoInfoAsyncCallback(null) {
//
//					@Override
//					protected void onInfoReceived(String node, Collection<Identity> identities, Collection<String> features) throws XMLException {
//						eventBus().fireEvent(new ServerFeaturesChangedEvent(identities, features));
//					}
//
//					public void onError(Stanza responseStanza, ErrorCondition error) throws JaxmppException {
//						throw new UnsupportedOperationException("Not supported yet.");
//					}
//
//					public void onTimeout() throws JaxmppException {
//						throw new UnsupportedOperationException("Not supported yet.");
//					}
//
//				});
//
////                                        if (be.getError() != null) {
////                                                Cookies.setCookie("username", 
////                                                        jaxmpp().getProperties().getUserProperty(SessionObject.USER_BARE_JID).toString(),
////                                                        new Date(new Date().getTime() + 24*60*60*1000*7));
////                                                Cookies.setCookie("password", 
////                                                        jaxmpp().getProperties().getUserProperty(SessionObject.PASSWORD).toString(),
////                                                        new Date(new Date().getTime() + 24*60*60*1000*7));
////                                        }
//				eventBus().fireEvent(new AuthEvent(bindedJid));
//			} catch (Exception ex) {
//				log.log(Level.WARNING, "exception firing auth event", ex);
//			}
//		}
//
//	}

}
