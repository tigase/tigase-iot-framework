/*
 * Movement.java
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

package tigase.iot.framework.values;

import tigase.iot.framework.devices.Value;

import java.time.LocalDateTime;

/**
 * Created by andrzej on 07.11.2016.
 */
public class Movement extends Value<Boolean> {

	public Movement(Boolean value) {
		super(value);
	}

	public Movement(Boolean value, LocalDateTime timestamp) {
		super(value, timestamp);
	}

	public boolean isMovementDetected() {
		return getValue();
	}

}
