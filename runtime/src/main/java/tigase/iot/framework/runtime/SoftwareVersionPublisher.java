/*
 * SoftwareVersionPublisher.java
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
package tigase.iot.framework.runtime;

import tigase.bot.RequiredXmppModules;
import tigase.bot.runtime.AbstractXmppBridge;
import tigase.jaxmpp.core.client.xmpp.modules.SoftwareVersionModule;
import tigase.jaxmpp.j2se.Jaxmpp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

@RequiredXmppModules({SoftwareVersionModule.class})
public class SoftwareVersionPublisher extends AbstractXmppBridge {

	private static final String[] _NIX_LIKE = { "linux", "mpe/ix", "freebsd", "irix", "digital unix", "unix" };

	private String hostname;
	private String operatingSystem;

	public SoftwareVersionPublisher() {
		try {
			hostname = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (Exception ex) {
			hostname = "Unknown";
		}
		operatingSystem = detectOperatingSystem();
	}

	@Override
	protected void jaxmppConnected(Jaxmpp jaxmpp) {
		jaxmpp.getSessionObject().setUserProperty(SoftwareVersionModule.OS_KEY, operatingSystem);
		jaxmpp.getSessionObject().setUserProperty(SoftwareVersionModule.NAME_KEY, hostname);
	}

	@Override
	protected void jaxmppDisconnected(Jaxmpp jaxmpp) {

	}

	private static String detectOperatingSystem() {
		String tmp = System.getProperty("os.name").toLowerCase();
		if (Arrays.stream(_NIX_LIKE).filter(like -> tmp.contains(like)).findFirst().isPresent()) {
			System.out.println("found linux!");
			Optional<String> version = Optional.ofNullable(getOsFromLsb());
			if (version.isPresent()) {
				return version.get();
			}
			version = Optional.ofNullable(getOsFromFile("/etc/system-release"));
			if (version.isPresent()) {
				return version.get();
			}
			version = getFileNamesStream("/etc", "-release").map(SoftwareVersionPublisher::getOsFromFile).filter(v -> v != null).findFirst();
			if (version.isPresent()) {
				return version.get();
			}
			version = getFileNamesStream("/etc", "_version").map(SoftwareVersionPublisher::getOsFromFile).filter(v -> v != null).findFirst();
			if (version.isPresent()) {
				return version.get();
			}
			version = Optional.ofNullable(getOsFromFile("/etc/issue"));
			if (version.isPresent()) {
				return version.get();
			}
		}
		return System.getProperty("os.name") + " " + System.getProperty("os.version");
	}

	private static String getOsFromLsb() {
		File f = new File("/etc/lsb-release");
		if (!f.exists()) {
			return null;
		}
		try {
			String desc = null;
			String codename = null;
			FileReader fileReader = new FileReader(f.getAbsolutePath());
			try (BufferedReader reader = new BufferedReader(fileReader)) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("DISTRIB_DESCRIPTION")) {
						desc = line.replace("DISTRIB_DESCRIPTION=", "").replace("\"", "");
					}
					if (line.startsWith("DISTRIB_CODENAME")) {
						codename = line.replace("DISTRIB_CODENAME=", "");
					}
				}
			}
			if (desc == null) {
				return null;
			}
			if (codename == null) {
				return desc;
			}
			return desc + " (" + codename + ")";
		} catch (IOException ex) {
			return null;
		}
	}

	private static String getOsFromFile(String filename) {
		File f = new File(filename);
		if (!f.exists()) {
			return null;
		}
		try {
			FileReader fileReader = new FileReader(f.getAbsolutePath());
			try (BufferedReader reader = new BufferedReader(fileReader)) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("PRETTY_NAME")) {
						return line.substring(13, line.length() - 1);
					}
				}
			}
			return null;
		} catch (IOException ex) {
			return null;
		}
	}

	private static Stream<String> getFileNamesStream(String path, String suffix) {
		File f = new File(path);
		if (f.exists()) {
			File[] files = f.listFiles();
			if (files != null) {
				return Arrays.stream(files).map(File::getAbsolutePath).filter(file -> file.endsWith(suffix));
			}
		}
		return Stream.empty();
	}
}
