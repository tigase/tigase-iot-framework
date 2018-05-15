package tigase.iot.framework.client.client.devices;

import tigase.iot.framework.client.Device;
import tigase.iot.framework.client.client.ClientFactory;

public class UnknownDevice extends DeviceRemoteConfigAware<Object, tigase.iot.framework.client.Device.IValue<Object>, tigase.iot.framework.client.Device<Device.IValue<Object>>> {

	public UnknownDevice(ClientFactory factory, Device<Device.IValue<Object>> sensor) {
		super(factory, "unknown", Icons.INSTANCE.unknown(), sensor);
	}

	@Override
	protected void setValue(Object value) {

	}
}
