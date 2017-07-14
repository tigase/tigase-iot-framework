/*
 * ISensor.java
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

package tigase.iot.framework.devices;

/**
 * Base interface implemented by <code>IDevice</code> classes which have a value,
 * ie. sensors from which data can be read (such as, ie. Thermometers) or have
 * some value (ie. light switch).
 *
 * Created by andrzej on 22.10.2016.
 */
public interface ISensor<V extends IValue>
		extends IDevice {

	/**
	 * Retrieve value of device.
	 *
	 * @return value
	 */
	V getValue();

}
