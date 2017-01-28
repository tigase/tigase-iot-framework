/*
 * DS1820DeviceType.java
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

package tigase.iot.framework.sensors.w1;

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
