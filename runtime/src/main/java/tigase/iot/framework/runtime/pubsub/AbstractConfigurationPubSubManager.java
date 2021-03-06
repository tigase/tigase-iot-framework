/*
 * AbstractConfigurationPubSubManager.java
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

package tigase.iot.framework.runtime.pubsub;

import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.iot.framework.devices.IConfigurationAware;
import tigase.iot.framework.devices.Value;
import tigase.iot.framework.devices.annotations.Advanced;
import tigase.iot.framework.devices.annotations.Fixed;
import tigase.iot.framework.devices.annotations.Hidden;
import tigase.iot.framework.devices.annotations.ValuesProvider;
import tigase.iot.framework.runtime.ConfigManager;
import tigase.iot.framework.runtime.DeviceNodesHelper;
import tigase.iot.framework.runtime.formatters.ConfigurationFormatter;
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
import tigase.kernel.core.Kernel;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Abstract implementation of configuration manager for PubSub-based device configuration storage.
 * 
 * Created by andrzej on 05.11.2016.
 */
public abstract class AbstractConfigurationPubSubManager<T extends IConfigurationAware>
		implements PubSubNodesManager.NodesObserver, PubSubNodesManager.PubSubNodeAware, Initializable,
				   UnregisterAware {

	private static final Logger log = Logger.getLogger(AbstractConfigurationPubSubManager.class.getCanonicalName());

	@ConfigField(desc = "Bean name")
	private String name;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ConfigurationFormatter formatter;

	@Inject
	private EventBus eventBus;

	@Inject(bean = "kernel")
	private Kernel kernel;

	@Inject(nullAllowed = true)
	private List<T> configurationAware = new ArrayList<>();

	@Inject(nullAllowed = true)
	private PubSubNodesManager pubSubNodesManager;

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

	/**
	 * Returns JID of PubSub service which will be used for storage.
	 * @param jaxmpp
	 * @return
	 */
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
					.filter(device -> oldConfigurationAware == null || !oldConfigurationAware.contains(device))
					.forEach(this::addConfigurationAware);

			if (pubSubNodesManager != null) {
				pubSubNodesManager.updateRequiredNodes();
				pubSubNodesManager.updateObservedNodes();
			}
		}
	}

	/**
	 * Register configuration aware device
	 * @param configurationAware
	 */
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

	/**
	 * Unregister configuration aware device
	 * @param configurationAware
	 */
	public void removeConfigurationAware(T configurationAware) {
		String deviceNodeName = getNodeNameForConfigurationAware(configurationAware);
		List<PubSubNodesManager.Node> toRemove = rootNodeInstance.getChildren()
				.stream()
				.filter(node -> node.getNodeName().equals(deviceNodeName))
				.collect(Collectors.toList());
		toRemove.forEach(rootNodeInstance::removeChild);
		configNodes.remove(deviceNodeName + "/config");
	}

	/**
	 * Get PubSub node name for configuration aware device.
	 * @param configurationAware
	 * @return
	 */
	public String getNodeNameForConfigurationAware(T configurationAware) {
		return rootNode + "/" + configurationAware.getName();
	}

	/**
	 * Get name of configuration aware device from PubSub node name.
	 * @param node
	 * @return
	 */
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

	protected abstract JabberDataElement prepareRootNodeConfig() throws JaxmppException;

	protected abstract JabberDataElement prepareNodeConfig(IConfigurationAware configurationAware) throws JaxmppException;

	protected abstract JabberDataElement prepareConfigNodeConfig(String collection) throws JaxmppException;

	protected void publishCurrentConfig(PubSubModule pubSubModule, JID pubSubJid, String nodeName, T device) {
		try {
			JabberDataElement data = new JabberDataElement(XDataType.form);
			for (Field field : BeanUtils.getAllFields(device.getClass())) {
				if ("label".equals(field.getName())) {
					continue;
				}
				
				ConfigField cf = field.getAnnotation(ConfigField.class);
				if (cf != null) {
					field.setAccessible(true);
					Hidden hidden = field.getAnnotation(Hidden.class);
					if (hidden != null) {
						Object value = field.get(device);
						data.addHiddenField(field.getName(), String.valueOf(value));
						continue;
					}
					Fixed fixed = field.getAnnotation(Fixed.class);
					if (fixed != null) {
						Object value = field.get(device);
						data.addFixedField(field.getName(), String.valueOf(value));
						continue;
					}
					ValuesProvider providerAnnotation = field.getAnnotation(ValuesProvider.class);
					if (providerAnnotation != null) {
						Object value = field.get(device);
						data.addFixedField(field.getName(), String.valueOf(value)).setLabel(cf.desc());
						continue;
					}
					boolean advanced = field.getAnnotation(Advanced.class) != null;
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
						if (advanced) {
							f.setAttribute("advanced", "true");
						}
					} else {
						Object value = field.get(device);
						tigase.jaxmpp.core.client.xmpp.forms.AbstractField f;
						if (value instanceof Boolean) {
							f = data.addBooleanField(field.getName(), (Boolean) value);
						} else {
							f = data.addTextSingleField(field.getName(), String.valueOf(value));
						}
						f.setLabel(cf.desc());     
						if (advanced) {
							f.setAttribute("advanced", "true");
						}
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
		if (!event.is(ConfigValue.class)) {
			return;
		}

		String beanName = DeviceNodesHelper.getDeviceIdFromNode(event.sourceId);
		applyConfigurion(beanName, ((ConfigValue) event.value).getValue());
	}

	protected void applyConfigurion(String beanName, JabberDataElement data) {
		configManager.updateBeanDefinition(beanName, config -> {
			log.fine(() -> "reconfiguring bean " + beanName + " with " +
					data.getFields().stream().collect(Collectors.toMap(field -> {
						try {
							return field.getVar();
						} catch (XMLException ex) {
							return null;
						}
					}, field -> {
						try {
							return field.getFieldValue();
						} catch (XMLException ex) {
							return null;
						}
					})));
			data.getFields().stream().forEach(field -> {
				try {
					config.put(field.getVar(), field.getFieldValue());
				} catch (XMLException e) {
					log.log(Level.WARNING, "could not read value of config field from form", e);
				}
			});
		});
		log.finest(() -> "reconfiguration of bean " + beanName + " completed");
	}

	public static class ConfigValue extends Value<JabberDataElement> {

		public ConfigValue(JabberDataElement value) {
			super(value);
		}

		public ConfigValue(JabberDataElement value, LocalDateTime timestamp) {
			super(value, timestamp);
		}

		@Override
		protected void toString(StringBuilder sb) {
			sb.append("config: ");
			if (getValue() == null) {
				sb.append("null");
			} else {
				try {
					sb.append(getValue().getAsString());
				} catch (XMLException ex) {
					sb.append("Exception: ").append(ex.getMessage());
				}
			}
		}
	}

}
