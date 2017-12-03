/*
 * ConfigManager.java
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
package tigase.iot.framework.runtime;

import tigase.bot.Autostart;
import tigase.component.DSLBeanConfigurator;
import tigase.conf.ConfigHelper;
import tigase.conf.ConfigReader;
import tigase.conf.ConfigWriter;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.xmpp.jid.JID;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean handles local configuration file. It is used to apply changes to configuration, reconfigure IoT framework
 * and save updated configuration to the local config file.
 */
@Autostart
@Bean(name = "configManager", parent = Kernel.class, active = false, exportable = true)
public class ConfigManager
		implements RegistrarBean, Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(ConfigManager.class.getCanonicalName());

	private final Map<String, Object> originalConfig = new HashMap<>();
	private final Map<String, Object> config = new HashMap<>();

	@ConfigField(desc = "Account credentials file", alias = "settings-file")
	private String configFile = "etc/config.tdsl";

	private Kernel kernel;

	public ConfigManager() {

	}

	@Override
	public void initialize() {
		File configFile = new File(this.configFile);
		if (configFile.exists()) {
			try {
				config.putAll(new ConfigReader().read(configFile));
			} catch (Exception ex) {
				throw new RuntimeException("failed to load local configuration file", ex);
			}
		}

		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				timer.cancel();
				originalConfig.putAll(kernel.getParent().getInstance(DSLBeanConfigurator.class).getProperties());
				applyConfigChanges();
			}
		}, 500);
	}

	@Override
	public void beforeUnregister() {

	}

	public void applyConfigChanges() {
		DSLBeanConfigurator configurator = this.kernel.getParent().getInstance(DSLBeanConfigurator.class);
		Map<String, Object> mergedConfig = ConfigHelper.merge(originalConfig, config);

		while (!ensureAtLeastOneXmppAccount(mergedConfig)) {
			mergedConfig = ConfigHelper.merge(mergedConfig, config);
		}

		configurator.setProperties(mergedConfig);
		configurator.configurationChanged();

		saveConfig();
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public void unregister(Kernel kernel) {
		this.kernel = null;
	}

	public void setBeanDefinition(AbstractBeanConfigurator.BeanDefinition beanDefinition) {
		config.put(beanDefinition.getBeanName(), beanDefinition);
		applyConfigChanges();
	}

	public boolean removeBeanDefinition(String name) {
		if (config.remove(name) != null) {
			applyConfigChanges();
			return true;
		}
		return false;
	}

	public void updateBeanDefinition(String beanName, Consumer<AbstractBeanConfigurator.BeanDefinition> consumer) {
		AbstractBeanConfigurator.BeanDefinition definition = (AbstractBeanConfigurator.BeanDefinition) config.get(beanName);
		if (definition != null) {
			consumer.accept(definition);
			applyConfigChanges();
		}
	}

	private boolean ensureAtLeastOneXmppAccount(Map<String, Object> config) {
		Map<String, Object> xmppService = (Map<String, Object>) config.get("xmppService");
		if (xmppService == null) {
			this.config.put("xmppService", new AbstractBeanConfigurator.BeanDefinition.Builder().name("xmppService").with(createAccountBean()).build());
		} else {
			if (xmppService.get("default") == null) {
				if (this.config.get("xmppService") == null) {
					this.config.put("xmppService", new AbstractBeanConfigurator.BeanDefinition.Builder().name("xmppService").with(createAccountBean()).build());
				} else {
					((Map<String, Object>) this.config.get("xmppService")).put("default", createAccountBean());
				}
			} else {
				return true;
			}
		}
		return false;
	}

	private AbstractBeanConfigurator.BeanDefinition createAccountBean() {
		return new AbstractBeanConfigurator.BeanDefinition.Builder().name("default")
				.active(true)
				.with("jid", JID.jidInstanceNS(UUID.randomUUID().toString(), "tigase-iot-hub.local", "iot"))
				.with("password", UUID.randomUUID().toString())
				.with("ignoreCertificateErrors", true)
				.with("register", true)
				.build();
	}

	private void saveConfig() {
		try {
			File configFile = new File(this.configFile);
			if (!configFile.getParentFile().exists()) {
				configFile.getParentFile().mkdirs();
			}
			new ConfigWriter().write(configFile, config);
		} catch (Exception ex) {
			log.log(Level.WARNING, "save of current configuration to file " + this.configFile + " failed", ex);
		}
	}
}
