package tigase.rpi.home.sensors.w1;

import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 24.10.2016.
 */
public class W1Master
		implements RegistrarBean, Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(W1Master.class.getCanonicalName());

	private final com.pi4j.io.w1.W1Master w1Master;

	@ConfigField(desc = "Miliseconds between reads")
	private long period;

	private Kernel parentKernel;
	private List<String> registeredDevices = new ArrayList<>();
	private Map<Class<? extends com.pi4j.io.w1.W1Device>, Class<? extends W1Device>> w1DeviceToBeanClass = new ConcurrentHashMap<>();

	public W1Master() {
		w1Master = new com.pi4j.io.w1.W1Master();
	}

	@Override
	public void beforeUnregister() {
	}

	@Override
	public void initialize() {
		w1Master.checkDeviceChanges();
		List<com.pi4j.io.w1.W1Device> deviceList = w1Master.getDevices();
		deviceList.forEach(w1Device -> {
			registerDeviceBean(w1Device);
		});
	}

	@Override
	public void register(Kernel kernel) {
		parentKernel = kernel.getParent();
	}

	@Override
	public void unregister(Kernel kernel) {
		registeredDevices.forEach(beanName -> parentKernel.unregister(beanName));
		parentKernel = null;
	}

	public void registerDeviceBean(com.pi4j.io.w1.W1Device device) {
		Class<? extends W1Device> beanClass = w1DeviceToBeanClass.get(device.getClass());
		if (beanClass == null) {
			return;
		}

		try {
			W1Device w1DeviceBean = beanClass.getConstructor(long.class, com.pi4j.io.w1.W1Device.class)
					.newInstance(period, device);
			String beanName = "w1-" + device.getId();
			parentKernel.registerBean(beanName).asInstance(w1DeviceBean).exec();
			registeredDevices.add(beanName);
		} catch (NoSuchMethodException|SecurityException|InstantiationException|IllegalAccessException|InvocationTargetException ex) {
			log.log(Level.WARNING, "Could not instantiate class " + beanClass.getCanonicalName(), ex);
		}
	}

}
