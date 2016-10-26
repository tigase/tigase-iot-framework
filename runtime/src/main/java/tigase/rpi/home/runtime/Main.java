package tigase.rpi.home.runtime;

import java.io.*;
import java.util.logging.LogManager;

/**
 * Created by andrzej on 22.10.2016.
 */
public class Main {

	public static void main(String[] args) throws Exception {
//		LogManager.getLogManager()
//				.readConfiguration(new ByteArrayInputStream(
//						("handlers=java.util.logging.ConsoleHandler\n" + ".level=FINE\n" + "tigase=WARNING\n" +
//								"tigase.jaxmpp=FINER\n" + "tigase.xml=FINE\n" +
//								"tigase.rpi.home.runtime=ALL\n" + "java.util.logging.ConsoleHandler.level=ALL\n" +
//								"java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\n" +
//								"java.util.logging.SimpleFormatter.format=%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %5$s%6$s%n")
//								.getBytes()));
		LogManager.getLogManager()
				.readConfiguration(new ByteArrayInputStream(
						("handlers=java.util.logging.ConsoleHandler,java.util.logging.FileHandler\n" + ".level=ALL\n" + "tigase=WARNING\n" +
								"tigase.jaxmpp=FINER\n" + "tigase.xml=FINE\n" + "tigase.kernel=ALL\n" +
								"tigase.rpi.home.runtime=ALL\n" + "java.util.logging.ConsoleHandler.level=ALL\n" +
								"java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\n" +
								"java.util.logging.FileHandler.level=ALL\n" + "java.util.logging.FileHandler.formatter=java.util.logging.SimpleFormatter\n" + "java.util.logging.FileHandler.pattern=home.log\n" +
								"java.util.logging.SimpleFormatter.format=%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %5$s%6$s%n")
								.getBytes()));

		File configFile = null;
		if (args.length > 0) {
			configFile = new File(args[0]);
		} else {
			configFile = File.createTempFile("config", "dsl");
			try (InputStream is = Main.class.getClassLoader().getResourceAsStream("etc/config.dsl")) {
				try (OutputStream os = new FileOutputStream(configFile)) {
					byte[] buf = new byte[1024];
					int read = 0;
					while ((read = is.read(buf, 0, buf.length)) != -1) {
						os.write(buf, 0, read);
					}
					os.flush();
					os.close();
				}
				is.close();
			}
			configFile.deleteOnExit();
		}

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.init(configFile);
		bootstrap.start();

		while (true) {
			Thread.sleep(2000);
		}
	}

}