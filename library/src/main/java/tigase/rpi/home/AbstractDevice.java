package tigase.rpi.home;

import tigase.eventbus.EventBus;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 22.10.2016.
 */
public abstract class AbstractDevice<T> implements IDevice<T> {

	private static final Logger log = Logger.getLogger(AbstractDevice.class.getCanonicalName());

	@ConfigField(desc = "Device name")
	private String name;

	@Inject
	private EventBus eventBus;

	private IValue<T> value;

	public String getName() {
		return name;
	}

	protected void fireEvent(Object event) {
		log.log(Level.FINEST, "{0}, firing event {1}", new Object[] { getName(), event });
		eventBus.fire(event);
	}

	public synchronized IValue<T> getValue() {
		return value;
	}

	protected synchronized void updateValue(IValue<T> newValue) {
		IValue<T> oldValue = value;
		this.value = newValue;
		fireValueChanged(oldValue, newValue);
	}

	protected void fireValueChanged(IValue<T> oldValue, IValue<T> newValue) {
		fireEvent(new ValueChangeEvent<T>(this, oldValue, newValue));
	}

	public static class ValueChangeEvent<T> {

		public final IDevice<T> source;
		public final IValue<T> oldValue;
		public final IValue<T> newValue;

		public ValueChangeEvent(IDevice<T> device, IValue<T> oldValue, IValue<T> newValue) {
			this.source = device;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		@Override
		public String toString() {
			return "ValueChangeEvent[source: " + source + ", oldValue: " + oldValue + ", newValue: " + newValue + "]";
		}
	}
}
