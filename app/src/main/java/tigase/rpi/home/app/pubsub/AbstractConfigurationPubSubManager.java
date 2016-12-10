package tigase.rpi.home.app.pubsub;

import tigase.bot.iot.Value;
import tigase.component.DSLBeanConfigurator;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.forms.TextMultiField;
import tigase.jaxmpp.core.client.xmpp.forms.XDataType;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubErrorCondition;
import tigase.jaxmpp.core.client.xmpp.modules.pubsub.PubSubModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.j2se.Jaxmpp;
import tigase.kernel.BeanUtils;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.Kernel;
import tigase.rpi.home.IConfigurationAware;
import tigase.rpi.home.app.formatters.ConfigurationFormatter;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 05.11.2016.
 */
public abstract class AbstractConfigurationPubSubManager<T extends IConfigurationAware>
		implements PubSubNodesManager.NodesObserver, PubSubNodesManager.PubSubNodeAware, Initializable,
				   UnregisterAware {

	private static final Logger log = Logger.getLogger(AbstractConfigurationPubSubManager.class.getCanonicalName());

	@ConfigField(desc = "Bean name")
	private String name;

	@Inject
	private DSLBeanConfigurator configurator;

	@Inject
	private ConfigurationFormatter formatter;

	@Inject
	private EventBus eventBus;

	@Inject(bean = "kernel")
	private Kernel kernel;

	@Inject
	private List<T> configurationAware;

	protected List<PubSubNodesManager.Node> requiredNodes = new ArrayList<>();
	protected List<String> configNodes = new ArrayList<>();

	@ConfigField(desc = "Root node")
	protected String rootNode;

	private PubSubNodesManager.Node rootNodeInstance;

	@Override
	public List<String> getObservedNodes() {
		return configurationAware.stream()
				.map(device -> rootNode + "/" + device.getName() + "/config")
				.collect(Collectors.toList());
	}

	protected JID getPubSubJID(Jaxmpp jaxmpp) {
		return JID.jidInstance(jaxmpp.getSessionObject().getUserBareJid());
	}

	public void setConfigurationAware(List<T> configurationAware) {
		List<T> oldConfigurationAware = this.configurationAware;
		this.configurationAware = configurationAware;

		if (rootNodeInstance != null) {
			if (oldConfigurationAware != null) {
				oldConfigurationAware.stream().filter(device -> !configurationAware.contains(device)).forEach(this::removeConfigurationAware);
			}
			this.configurationAware.stream()
					.filter(device -> oldConfigurationAware == null || oldConfigurationAware.contains(device))
					.forEach(this::addConfigurationAware);
		}
	}

	public void addConfigurationAware(T configurationAware) {
		try {
			String nodeName = getNodeNameForConfigurationAware(configurationAware);
			PubSubNodesManager.Node node = new PubSubNodesManager.Node(nodeName, prepareNodeConfig(configurationAware));
			PubSubNodesManager.Node configNode = new PubSubNodesManager.Node(nodeName + "/config",
																			 prepareConfigNodeConfig(nodeName));
			node.addChild(configNode);
			rootNodeInstance.addChild(node);
			configNodes.add(nodeName + "/config");
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, "could not add child node for configuration aware bean " + configurationAware, ex);
		}
	}

	public void removeConfigurationAware(T configurationAware) {
		String deviceNodeName = getNodeNameForConfigurationAware(configurationAware);
		List<PubSubNodesManager.Node> toRemove = rootNodeInstance.getChildren()
				.stream()
				.filter(node -> node.getNodeName().equals(deviceNodeName))
				.collect(Collectors.toList());
		toRemove.forEach(rootNodeInstance::removeChild);
		configNodes.remove(deviceNodeName + "/config");
	}

	public String getNodeNameForConfigurationAware(T configurationAware) {
		return rootNode + "/" + configurationAware.getName();
	}

	public String getConfigurationAwareNameFromNode(String node) {
		String[] parts = node.split("/");
		if (parts.length > 1) {
			return parts[1];
		}
		return null;
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
		try {
			rootNodeInstance = new PubSubNodesManager.Node(rootNode, prepareRootNodeConfig());
			configurationAware.forEach(this::addConfigurationAware);
			requiredNodes.add(rootNodeInstance);
		} catch (JaxmppException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	@Override
	public List<PubSubNodesManager.Node> getRequiredNodes() {
		return requiredNodes;
	}

	@HandleEvent
	public void onPubSubNodesReady(PubSubNodesManager.NodeReady event) {
		if (configNodes.contains(event.node)) {
			String name = getConfigurationAwareNameFromNode(event.node);
			configurationAware.stream().filter(aware -> aware.getName().equals(name)).forEach(aware -> {
				try {
					PubSubModule pubSubModule = event.jaxmpp.getModule(PubSubModule.class);
					pubSubModule.retrieveItem(event.pubSubJid.getBareJid(), event.node, null, 1,
											  new PubSubModule.RetrieveItemsAsyncCallback() {
												  @Override
												  protected void onRetrieve(IQ responseStanza, String nodeName,
																			Collection<Item> items) {
													  if (items.isEmpty()) {
														  publishCurrentConfig(pubSubModule, event.pubSubJid, nodeName,
																			   aware);
													  } else {
														  Item item = items.iterator().next();
														  try {
															  ConfigValue config = formatter.fromElement(item.getPayload());
															  if (config == null || config.getValue().getField("type") == null) {
															  	  publishCurrentConfig(pubSubModule, event.pubSubJid, nodeName, aware);
															  } else {
																  applyConfigurion(aware.getName(), config.getValue());
															  }
														  } catch (JaxmppException ex) {
															  log.log(Level.WARNING,
																	  "failed to parse retrieved configuration", ex);
														  }
													  }
												  }

												  @Override
												  protected void onEror(IQ response,
																		XMPPException.ErrorCondition errorCondition,
																		PubSubErrorCondition pubSubErrorCondition)
														  throws JaxmppException {
													  log.log(Level.WARNING,
															  "could not read configuration from node " + event.node +
																	  " = " + errorCondition + ", " +
																	  pubSubErrorCondition);
												  }

												  @Override
												  public void onTimeout() throws JaxmppException {
													  // ignoring
												  }
											  });
				} catch (JaxmppException ex) {

				}
			});
		}
	}

	protected JabberDataElement prepareRootNodeConfig() throws JaxmppException {
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", "Devices");
		config.addTextSingleField("pubsub#node_type", "collection");
		config.addTextSingleField("pubsub#access_model", "presence");
		config.addTextSingleField("pubsub#persist_items", "1");

		return config;
	}

	protected JabberDataElement prepareNodeConfig(IConfigurationAware configurationAware) throws JaxmppException {
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", configurationAware.getName());
		config.addTextSingleField("pubsub#node_type", "collection");
		config.addTextSingleField("pubsub#access_model", "presence");
		config.addTextSingleField("pubsub#persist_items", "1");
		config.addTextSingleField("pubsub#collection", rootNode);

		return config;
	}

	protected JabberDataElement prepareConfigNodeConfig(String collection) throws JaxmppException {
		JabberDataElement config = new JabberDataElement(XDataType.submit);
		config.addTextSingleField("pubsub#title", "Configuration");
		config.addTextSingleField("pubsub#node_type", "leaf");
		config.addTextSingleField("pubsub#access_model", "presence");
		config.addTextSingleField("pubsub#persist_items", "1");
		config.addTextSingleField("pubsub#collection", collection);

		return config;
	}

	protected void publishCurrentConfig(PubSubModule pubSubModule, JID pubSubJid, String nodeName, T device) {
		try {
			JabberDataElement data = new JabberDataElement(XDataType.form);
			for (Field field : BeanUtils.getAllFields(device.getClass())) {
				ConfigField cf = field.getAnnotation(ConfigField.class);
				if (cf != null) {
					field.setAccessible(true);
					if (Collection.class.isAssignableFrom(field.getType())) {
						TextMultiField f = data.addTextMultiField(field.getName());
						f.setLabel(cf.desc());
						Collection collectionOfValues = (Collection) field.get(device);
						if (collectionOfValues != null) {
							String[] values = (String[]) collectionOfValues.stream()
									.map(value -> value.toString())
									.toArray(String[]::new);
							f.setFieldValue(values);
						}
					} else {
						Object value = field.get(device);
						tigase.jaxmpp.core.client.xmpp.forms.Field f;
						if (value instanceof Boolean) {
							f = data.addBooleanField(field.getName(), (Boolean) value);
						} else {
							f = data.addTextSingleField(field.getName(), String.valueOf(value));
						}
						f.setDesc(cf.desc());
					}
				}
			}

			Element payload = formatter.toElement(new ConfigValue(data));

			pubSubModule.publishItem(pubSubJid.getBareJid(), nodeName, null, payload,
									 new PubSubModule.PublishAsyncCallback() {
										 @Override
										 public void onPublish(String itemId) {
											 log.log(Level.INFO,
													 "published configuration to " + pubSubJid + " at node " +
															 nodeName + " with id " + itemId);
										 }

										 @Override
										 protected void onEror(IQ response, XMPPException.ErrorCondition errorCondition,
															   PubSubErrorCondition pubSubErrorCondition)
												 throws JaxmppException {
											 log.log(Level.WARNING,
													 "publication of configuration to " + pubSubJid + " at node " +
															 nodeName + " failed: " + errorCondition + ", " +
															 pubSubErrorCondition);
										 }

										 @Override
										 public void onTimeout() throws JaxmppException {
											 log.log(Level.WARNING,
													 "publication of configuration to " + pubSubJid + " at node " +
															 nodeName + " timed out");
										 }
									 });
		} catch (JaxmppException ex) {
			log.log(Level.WARNING, "failed to prepare form with component configuration", ex);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@HandleEvent
	public void onConfigurationChangedEvent(ExtendedPubSubNodesManager.ValueChangedEvent event) {
		if (!event.is(JabberDataElement.class)) {
			return;
		}

		applyConfigurion(event.sourceId, (JabberDataElement) event.value);
	}

	protected void applyConfigurion(String beanName, JabberDataElement data) {
		BeanConfig bc = getBeanConfigForName(beanName, kernel);
		log.finest(() -> "found bean " + bc + " for reconfiguration");
		if (bc != null) {
			Map<String, Object> props = configurator.getProperties();
			ArrayDeque<String> path = new ArrayDeque<>();
			path.offer(bc.getBeanName());

			Kernel kernel = bc.getKernel();
			do {
				if (kernel == this.kernel) {
					break;
				}

				path.offer(kernel.getName());
			} while (kernel.getParent() != this.kernel);

			String name;
			while ((name = path.pollLast()) != null) {
				props = (Map<String, Object>) props.computeIfAbsent(name, (k) -> new HashMap<>());
			}

			log.finest(() -> "updating configuration for " + bc.getBeanName());
			final Map<String, Object> config = props;
			data.getFields().stream().forEach(field -> {
				try {
					config.put(field.getName(), field.getFieldValue());
				} catch (XMLException e) {
					log.log(Level.WARNING, "could not read value of config field from form", e);
				}
			});

			log.finest(() -> "reconfiguring bean " + bc.getBeanName());
			Object bean = bc.getKernel().getInstance(bc.getBeanName());
			configurator.configure(bc, bean);
			log.finest(() -> "reconfiguration of bean " + bc.getBeanName() + " completed");
		}
	}

	private BeanConfig getBeanConfigForName(String name, Kernel kernel) {
		BeanConfig bc = kernel.getDependencyManager().getBeanConfig(name);
		if (bc != null) {
			return bc;
		}
		for (String subkernelName : kernel.getNamesOf(Kernel.class)) {
			Kernel subkernel = kernel.getInstance(subkernelName);
			if (subkernel == kernel) {
				continue;
			}

			if (subkernel != null) {
				bc = getBeanConfigForName(name, subkernel);
				if (bc != null) {
					return bc;
				}
			}
		}

		return null;
	}

	public static class ConfigValue extends Value<JabberDataElement> {

		public ConfigValue(JabberDataElement value) {
			super(value);
		}

		public ConfigValue(JabberDataElement value, LocalDateTime timestamp) {
			super(value, timestamp);
		}
	}

}
