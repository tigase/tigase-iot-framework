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
