package tigase.rpi.home.app.utils;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;

import static java.time.temporal.ChronoField.*;

/**
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
