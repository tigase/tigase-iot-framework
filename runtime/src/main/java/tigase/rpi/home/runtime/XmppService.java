package tigase.rpi.home.runtime;

import tigase.eventbus.EventBus;
import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceStore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.DefaultRosterStore;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterStore;
import tigase.jaxmpp.j2se.J2SEPresenceStore;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.kernel.beans.*;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.kernel.core.Kernel;
import tigase.rpi.home.Autostart;
import tigase.rpi.home.XmppBridge;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 22.10.2016.
 */
@Bean(name = "xmppService", parent = Kernel.class)
@Autostart
public class XmppService
		implements tigase.rpi.home.XmppService, RegistrarBeanWithDefaultBeanClass {

	private final Map<String, Jaxmpp> instancesByName = new ConcurrentHashMap<>();

	private final Map<SessionObject, Jaxmpp> instancesBySessionObject = new ConcurrentHashMap<>();

	private final List<Jaxmpp> anonymousInstances = new ArrayList<>();

	@Inject(nullAllowed = true)
	private ArrayList<JaxmppBean> jaxmppBeans = new ArrayList<>();

	@Inject(nullAllowed = true)
	private ArrayList<XmppBridge> xmppBridges = new ArrayList<>();

	@Override
	public Jaxmpp getConnection(String name) {
		return getNamedConnections().get(name);
	}

	@Override
	public Jaxmpp getConnection(SessionObject sessionObject) {
		return instancesBySessionObject.get(sessionObject);
	}

	@Override
	public Class<?> getDefaultBeanClass() {
		return JaxmppBean.class;
	}

	public Map<String, Jaxmpp> getNamedConnections() {
		return Collections.unmodifiableMap(instancesByName);
	}

	public Collection<Jaxmpp> getAnonymousConnections() {
		return anonymousInstances;
	}

	@Override
	public Collection<Jaxmpp> getAllConnections() {
		List<Jaxmpp> result = new ArrayList<>(instancesByName.values());
		result.addAll(anonymousInstances);
		return result;
	}

	@Override
	public void register(Kernel kernel) {
	}

	@Override
	public void unregister(Kernel kernel) {

	}

	public void setJaxmppBeans(ArrayList<JaxmppBean> jaxmppBeans) {
		if (jaxmppBeans == null) {
			jaxmppBeans = new ArrayList<>();
		}
		HashSet<JaxmppBean> oldBeans = new HashSet<>(this.jaxmppBeans);
		HashSet<JaxmppBean> newBeans = new HashSet<>(jaxmppBeans);

		oldBeans.stream().filter(b -> !newBeans.contains(b)).forEach(bean -> {
			instancesByName.remove(bean.name);
			instancesBySessionObject.remove(bean.jaxmpp.getSessionObject());
		});

		newBeans.stream().filter(b -> !oldBeans.contains(b)).forEach(bean -> {
			instancesByName.put(bean.name, bean.jaxmpp);
			instancesBySessionObject.put(bean.jaxmpp.getSessionObject(), bean.jaxmpp);
		});

		this.jaxmppBeans = jaxmppBeans;
	}

	public void setXmppBridges(ArrayList<XmppBridge> xmppBridges) {
		if (xmppBridges == null) {
			xmppBridges = new ArrayList<>();
		}
		Set<Class<? extends XmppModule>> oldRequiredModules = new HashSet<>();
		Set<Class<? extends XmppModule>> newRequiredModules = new HashSet<>();
		this.xmppBridges.forEach(xmppBridge -> oldRequiredModules.addAll(xmppBridge.getRequiredXmppModules()));
		xmppBridges.forEach(xmppBridge -> newRequiredModules.addAll(xmppBridge.getRequiredXmppModules()));

		this.xmppBridges = xmppBridges;

		oldRequiredModules.stream()
				.filter(cls -> !newRequiredModules.contains(cls))
				.forEach(cls -> unregisterXmppModule(cls));
		newRequiredModules.stream()
				.filter(cls -> !oldRequiredModules.contains(cls))
				.forEach(cls -> registerXmppModule(cls));
	}

	protected void registerXmppModule(Class<? extends XmppModule> moduleClass) {
		jaxmppBeans.forEach(jaxmppBean -> jaxmppBean.registerXmppModule(moduleClass));
	}

	protected void unregisterXmppModule(Class<? extends XmppModule> moduleClass) {
		jaxmppBeans.forEach(jaxmppBean -> jaxmppBean.unregisterXmppModule(moduleClass));
	}

	protected Set<Class<? extends XmppModule>> getRequiredModules() {
		return xmppBridges.stream()
				.map(xmppBridge -> xmppBridge.getRequiredXmppModules())
				.flatMap(Set::stream)
				.collect(Collectors.toSet());
	}

	public static class JaxmppBean
			implements Initializable,
					   UnregisterAware,
					   JaxmppCore.LoggedInHandler,
					   JaxmppCore.LoggedOutHandler,
					   ConfigurationChangedAware {

		private static final Logger log = Logger.getLogger(JaxmppBean.class.getCanonicalName());

		@ConfigField(desc = "Name")
		private String name;

		@ConfigField(desc = "JID")
		private JID jid;

		@ConfigField(desc = "Domain")
		private String domain;

		@ConfigField(desc = "Password")
		private String password;

		@ConfigField(desc = "Ignore TLS/SSL certificate errors")
		private boolean ignoreCertificateErrors;

		@Inject
		private EventBus eventBus;

		@Inject
		private ScheduledExecutorService scheduledExecutorService;

		@Inject
		private XmppService xmppService;

		private final Jaxmpp jaxmpp = new Jaxmpp();
		private ScheduledFuture scheduledTask;

		private int retry = 0;
		private boolean shutdown = false;

		public JaxmppBean() {
			jaxmpp.getEventBus().addHandler(JaxmppCore.LoggedInHandler.LoggedInEvent.class, this);
			jaxmpp.getEventBus().addHandler(JaxmppCore.LoggedOutHandler.LoggedOutEvent.class, this);
		}

		@Override
		public void beanConfigurationChanged(Collection<String> collection) {
			if (!jaxmpp.isConnected()) {
				return;
			}

			if (collection.contains("jid") || collection.contains("password") || collection.contains("domain")) {
				try {
					jaxmpp.disconnect(false);
				} catch (Exception ex) {
					log.log(Level.WARNING, "Failed to disconnect client " + name, ex);
				}
			}
		}

		@Override
		public void initialize() {
			if (scheduledTask == null || jaxmpp.getConnector().getState() == Connector.State.disconnected) {
				xmppService.getRequiredModules().forEach(moduleCls -> {
					registerXmppModule(moduleCls);
				});
				eventBus.fire(new JaxmppAddedEvent(jaxmpp));
				scheduleReconnection();
			}
		}

		@Override
		public void beforeUnregister() {
			try {
				shutdown = true;
				if (scheduledTask != null) {
					scheduledTask.cancel(true);
				}
				eventBus.fire(new JaxmppRemovedEvent(jaxmpp));
				log.log(Level.INFO, "Disconnecting client " + name);
				jaxmpp.disconnect(false);
			} catch (JaxmppException ex) {
				log.log(Level.WARNING, "Failed to disconnect client " + name, ex);
			}
		}

		@Override
		public void onLoggedIn(SessionObject sessionObject) {
			log.log(Level.INFO, "Client connected " + name);
			eventBus.fire(new JaxmppConnectedEvent(jaxmpp));
		}

		@Override
		public void onLoggedOut(SessionObject sessionObject) {
			if (retry == 0) {
				eventBus.fire(new JaxmppDisconnectedEvent(jaxmpp));
			}
			retry++;
			scheduleReconnection();
		}

		protected void registerXmppModule(Class<? extends XmppModule> moduleClass) {
			if (jaxmpp.getModulesManager().getModule(moduleClass) == null) {
				try {
					if (PresenceModule.class.isAssignableFrom(moduleClass)) {
						PresenceStore presenceStore = PresenceModule.getPresenceStore(jaxmpp.getSessionObject());
						if (presenceStore == null) {
							presenceStore = new J2SEPresenceStore();
							PresenceModule.setPresenceStore(jaxmpp.getSessionObject(), presenceStore);
						}
						jaxmpp.set((Property) presenceStore);
					}
					if (RosterModule.class.isAssignableFrom(moduleClass)) {
						RosterStore rosterStore = RosterModule.getRosterStore(jaxmpp.getSessionObject());
						if (rosterStore == null) {
							rosterStore = new DefaultRosterStore();
							RosterModule.setRosterStore(jaxmpp.getSessionObject(), rosterStore);
						}
						jaxmpp.set((Property) rosterStore);
					}
					jaxmpp.getModulesManager().register(moduleClass.newInstance());
				} catch (InstantiationException | IllegalAccessException ex) {
					log.log(Level.WARNING, "Failed to register module " + moduleClass, ex);
				}
			}
		}

		protected void unregisterXmppModule(Class<? extends XmppModule> moduleClass) {
			XmppModule module = jaxmpp.getModulesManager().getModule(moduleClass);
			if (module != null) {
				jaxmpp.getModulesManager().unregister(module);
			}
		}

		protected void scheduleReconnection() {
			if (shutdown) {
				return;
			}

			scheduledTask = scheduledExecutorService.schedule(() -> {
				try {
					if (domain != null) {
						jaxmpp.getConnectionConfiguration().setUserJID((BareJID) null);
						jaxmpp.getConnectionConfiguration().setServer(domain);
						jaxmpp.getConnectionConfiguration().setUserPassword(null);
					} else {
						jaxmpp.getConnectionConfiguration().setServer(null);
						jaxmpp.getConnectionConfiguration().setUserJID(jid.getBareJid());
						jaxmpp.getConnectionConfiguration().setResource(jid.getResource());
						jaxmpp.getConnectionConfiguration().setUserPassword(password);
					}
					if (ignoreCertificateErrors) {
						jaxmpp.getSessionObject()
								.setUserProperty(Connector.TRUST_MANAGERS_KEY,
												 new X509TrustManager[]{new X509TrustManager() {
													 @Override
													 public void checkClientTrusted(X509Certificate[] x509Certificates,
																					String s)
															 throws CertificateException {

													 }

													 @Override
													 public void checkServerTrusted(X509Certificate[] x509Certificates,
																					String s)
															 throws CertificateException {

													 }

													 @Override
													 public X509Certificate[] getAcceptedIssuers() {
														 return new X509Certificate[0];
													 }
												 }});
					} else {
						jaxmpp.getSessionObject().setUserProperty(Connector.TRUST_MANAGERS_KEY, null);
					}
					jaxmpp.login();
				} catch (JaxmppException ex) {
					log.log(Level.WARNING, "Can not connect client " + name, ex);
					this.onLoggedOut(null);
				}
			}, retry, TimeUnit.SECONDS);
		}

	}

	public static class JaxmppConnectedEvent {

		public final Jaxmpp jaxmpp;

		public JaxmppConnectedEvent(Jaxmpp jaxmpp) {
			this.jaxmpp = jaxmpp;
		}

	}

	public static class JaxmppDisconnectedEvent {

		public final Jaxmpp jaxmpp;

		public JaxmppDisconnectedEvent(Jaxmpp jaxmpp) {
			this.jaxmpp = jaxmpp;
		}

	}

	public static class JaxmppAddedEvent {

		public final Jaxmpp jaxmpp;

		public JaxmppAddedEvent(Jaxmpp jaxmpp) {
			this.jaxmpp = jaxmpp;
		}

	}

	public static class JaxmppRemovedEvent {

		public final Jaxmpp jaxmpp;

		public JaxmppRemovedEvent(Jaxmpp jaxmpp) {
			this.jaxmpp = jaxmpp;
		}

	}

}
