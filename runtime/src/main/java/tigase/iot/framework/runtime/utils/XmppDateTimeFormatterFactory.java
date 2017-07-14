/*
 * XmppDateTimeFormatterFactory.java
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

package tigase.iot.framework.runtime.utils;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;

import static java.time.temporal.ChronoField.*;

/**
 * Factory provides instances of date time formatter configured to properly
 * parse and format data and time to XMPP compatible date time format.
 *
 * Created by andrzej on 30.10.2016.
 */
public class XmppDateTimeFormatterFactory {

	public static DateTimeFormatter newInstance() {
		return new DateTimeFormatterBuilder().parseCaseInsensitive()
				.appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
				.appendLiteral('-')
				.appendValue(MONTH_OF_YEAR, 2)
				.appendLiteral('-')
				.appendValue(DAY_OF_MONTH, 2)
				.appendLiteral('T')
				.appendValue(HOUR_OF_DAY, 2)
				.appendLiteral(':')
				.appendValue(MINUTE_OF_HOUR, 2)
				.appendLiteral(':')
				.appendValue(SECOND_OF_MINUTE, 2)
				.optionalStart()
				.appendLiteral('.')
				.appendFraction(MILLI_OF_SECOND, 3, 3, false)
				.optionalEnd()
				.appendOffsetId().toFormatter();
	}

}
