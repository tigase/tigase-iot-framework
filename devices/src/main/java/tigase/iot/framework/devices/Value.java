/*
 * Value.java
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

import java.time.LocalDateTime;

/**
 * Created by andrzej on 22.10.2016.
 */
public class Value<T>
		implements IValue<T> {

	private final LocalDateTime timestamp;
	private final T value;

	public Value(T value) {
		this(value, LocalDateTime.now());
	}

	public Value(T value, LocalDateTime timestamp) {
		this.value = value;
		this.timestamp = timestamp;
	}

	@Override
	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	@Override
	public T getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "Value(" + value + ")";
	}
}
