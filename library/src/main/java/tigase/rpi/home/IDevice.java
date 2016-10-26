package tigase.rpi.home;

/**
 * Created by andrzej on 22.10.2016.
 */
public interface IDevice<V> {

	String getName();

	IValue<V> getValue();

}
