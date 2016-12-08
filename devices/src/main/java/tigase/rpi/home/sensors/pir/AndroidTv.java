package tigase.rpi.home.sensors.pir;

import tigase.bot.AbstractPeriodDevice;
import tigase.kernel.beans.config.ConfigField;
import tigase.rpi.home.IConfigurationAware;
import tigase.rpi.home.values.Movement;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 02.12.2016.
 */
public class AndroidTv
		extends AbstractPeriodDevice<Movement>
		implements IConfigurationAware {

	private static final Logger log = Logger.getLogger(AndroidTv.class.getCanonicalName());

	@ConfigField(desc = "HTTP URL for TV Web API")
	protected String address;

	public AndroidTv() {
		super("tv-sensor", 60 * 1000);
	}

	@Override
	protected Movement readValue() {
		try {
			URL url = new URL(address);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");

			con.setDoOutput(true);
			DataOutputStream writer = new DataOutputStream(con.getOutputStream());
			writer.writeBytes("{\"id\":2,\"method\":\"getPowerStatus\",\"version\":\"1.0\",\"params\":[]}");
			writer.flush();
			writer.close();

			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				return null;
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			reader.close();

			boolean enabled = sb.toString().contains("{\"status\":\"active\"}");
			return new Movement(enabled);
		} catch (Exception ex) {
			log.log(Level.WARNING, getName() + ", could not read state from " + address, ex);
			return null;
		}
	}
}
