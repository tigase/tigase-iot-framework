package tigase.rpi.home.runtime;

import tigase.component.DSLBeanConfigurator;
import tigase.conf.ConfigReader;
import tigase.eventbus.EventBusFactory;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.Kernel;
import tigase.rpi.home.Autostart;
import tigase.rpi.home.sensors.light.BH1750;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 22.10.2016.
 */
public class Bootstrap {

	private static final Logger log = Logger.getLogger(Bootstrap.class.getCanonicalName());

	private final Kernel kernel = new Kernel("root");

	private final ArrayList<String> packageFilter = new ArrayList<>();

	private final Map<String, Object> props = new ConcurrentHashMap<>();

	public void init(File configFile)
			throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		props.putAll(new ConfigReader().read(configFile));

		kernel.registerBean(CustomTypesConverter.class).exec();
		kernel.registerBean("classUtilBean")
				.asInstance(new ClassUtilBean())
				.exportable()
				.exec();
		kernel.registerBean(DSLBeanConfigurator.class).exec();
		kernel.registerBean("scheduledExecutorService")
				.asInstance(Executors.newScheduledThreadPool(4))
				.exportable()
				.exec();

		kernel.registerBean("eventBus").asInstance(EventBusFactory.getInstance()).exportable().exec();
		kernel.registerBean("xmppService").asClass(XmppService.class).exec();

		kernel.getInstance(DSLBeanConfigurator.class).setProperties(props);

		kernel.getInstance(DSLBeanConfigurator.class).registerBeans((BeanConfig)null, (Object)null, this.props);
	}

	public void start() {
		kernel.getDependencyManager().getBeanConfigs().stream().forEach(bc -> {
			log.log(Level.SEVERE, "1: found bean config " + bc.getBeanName() + ", class = " + bc.getClazz());
		});

		initializeAutostartBeans(kernel);

		kernel.getDependencyManager().getBeanConfigs().stream().forEach(bc -> {
			log.log(Level.SEVERE, "2: found bean config " + bc.getBeanName() + ", class = " + bc.getClazz());
		});

		log.log(Level.SEVERE, "Light level, lux = " + ((BH1750) kernel.getInstance("lightSensor")).getValue());
		kernel.getInstance("test12");
	}

	public void stop() {

	}

	private void initializeAutostartBeans(Kernel kernel) {
		if (log.isLoggable(Level.FINE)) {
			log.fine("Starting 'autostart' beans in kernel " + kernel.getName());
		}
		for (BeanConfig bc : kernel.getDependencyManager().getBeanConfigs()) {
			if (Kernel.class.isAssignableFrom(bc.getClazz())) {
				Kernel sk = kernel.getInstance(bc.getBeanName());
				if (sk != kernel) {
					initializeAutostartBeans(sk);
					return;
				}
			}
			Autostart autostart = bc.getClazz().getAnnotation(Autostart.class);
			if (bc.getState() == BeanConfig.State.registered && autostart != null) {
				if (log.isLoggable(Level.FINE)) {
					log.fine("Autostarting bean " + bc);
				}
				kernel.getInstance(bc.getBeanName());
			}
		}

	}

	private static class ClassUtilBean extends tigase.util.ClassUtilBean {

		private boolean isPackageAllowed(final String packageName) {
			if (packageName.startsWith("tigase.") && !packageName.startsWith("tigase.rpi.")) {
				return false;
			}

			return true;
		}

		@Override
		public Set<Class<?>> getAllClasses() {
			return super.getAllClasses().stream().filter(aClass -> isPackageAllowed(aClass.getName())).collect(
					Collectors.toSet());
		}
	}
}
