package tigase.rpi.home.sensors.w1;

import com.pi4j.io.w1.W1Device;
import com.pi4j.io.w1.W1DeviceType;

import java.io.File;

/**
 * Created by andrzej on 24.10.2016.
 */
public class DS1820DeviceType implements W1DeviceType {

	public static final int FAMILY_CODE = 0x28;

	@Override
	public int getDeviceFamilyCode() {
		return FAMILY_CODE;
	}

	@Override
	public Class<? extends W1Device> getDeviceClass() {
		return DS1820.W1Device.class;
	}

	@Override
	public W1Device create(File deviceDir) {
		return new DS1820.W1Device(deviceDir);
	}
}
