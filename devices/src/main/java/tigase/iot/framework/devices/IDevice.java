/*
 * IDevice.java
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

import java.util.Collection;
import java.util.Collections;

/**
 * Interface implemented by every devices used within Tigase IoT framework.
 * 
 * Created by bmalkow on 08.12.2016.
 */
public interface IDevice {

	/**
	 * Retrieve name of a device.
	 *
	 * @return name of a device
	 */
	String getName();

	/**
	 * Collection of categories for which this device will be available, ie. "switch", "lights-ceiling", etc.
	 * @return
	 */
	default Collection<Category> getCategories() {
		if (getName() == null || getType() == null) {
			throw new IllegalStateException("Name or type not set!");
		}
		return Collections.singleton(new Category(getType(), getName()));
	}

	String getType();

	default String getLabel() {
		return getName();
	}
	
	class Category {

		private final String name;
		private final String id;

		public Category(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public String getId() {
			return id;
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Category) {
				return id.equals(((Category) obj).id);
			}
			return false;
		}
	}
	
}
