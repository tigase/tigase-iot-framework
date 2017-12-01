/*
 * ValuesProvider.java
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

package tigase.iot.framework;

import tigase.kernel.core.Kernel;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Interface which needs to be implemented by beans which will be used as a values providers for a fields annotated with
 * <code>@ConfigField</code> and <code>@ValuesProvider</code> annotations.
 */
public interface ValuesProvider {

	List<ValuePair> getValuesFor(Object bean, Field field, Kernel kernel);

	/**
	 * Implementation of a value pairs to provide a list of values and labels for them.
	 */
	class ValuePair {

		private final String label;
		private final String value;

		public ValuePair(String value, String label) {
			this.value = value;
			this.label = label;
		}

		public String getValue() {
			return value;
		}

		public String getLabel() {
			return label;
		}

	}
}
