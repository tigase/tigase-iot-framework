package tigase.rpi.home.app.formatters;

import tigase.bot.iot.IValue;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.rpi.home.app.ValueFormatter;
import tigase.rpi.home.app.utils.XmppDateTimeFormatterFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by andrzej on 05.11.2016.
 */
public abstract class AbstractValueFormatter<T extends IValue>
		implements ValueFormatter<T> {

	protected final String name;
	private final DateTimeFormatter formatter = XmppDateTimeFormatterFactory.newInstance();
	private final Class<T> supportedClass;

	public AbstractValueFormatter(String name, Class<T> supportedClass) {
		this.name = name;
		this.supportedClass = supportedClass;
	}

	@Override
	public Class<T> getSupportedClass() {
		return supportedClass;
	}

	protected Element createTimestampElement(T value) throws XMLException {
		Element timestampElem = ElementFactory.create("timestamp");
		timestampElem.setAttribute("value", value.getTimestamp().atZone(ZoneId.systemDefault()).format(formatter));
		return timestampElem;
	}

	protected LocalDateTime parseTimestampElement(Element elem) throws XMLException {
		if (!"timestamp".equals(elem.getName()) || elem.getAttribute("value") == null) {
			return null;
		}

		return formatter.parse(elem.getAttribute("value")).query(ZonedDateTime::from).toLocalDateTime();
	}
}
