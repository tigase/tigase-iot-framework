package tigase.rpi.home;

import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 22.10.2016.
 */
public abstract class AbstractPeriodDevice<T> extends AbstractDevice<T> implements Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(AbstractPeriodDevice.class.getCanonicalName());

	@ConfigField(desc = "Miliseconds between reads")
	private long period;

	@Inject
	protected ScheduledExecutorService scheduledExecutorService;

	private ScheduledFuture future;

	protected AbstractPeriodDevice(long period) {
		this.period = period;
	}

	protected abstract T readValue();

	protected void refresh() {
		log.log(Level.FINEST, "{0}, refreshing..", getName());
		T val = readValue();
		log.log(Level.FINEST, "{0}, got data: {1}", new Object[] { getName(), val });
		updateValue(new Value(val));
	}

	@Override
	public void initialize() {
		future = scheduledExecutorService.scheduleAtFixedRate(() -> refresh(), 0, period, TimeUnit.MILLISECONDS);
	}

	@Override
	public void beforeUnregister() {
		if (future != null) {
			future.cancel(false);
		}
	}
}
