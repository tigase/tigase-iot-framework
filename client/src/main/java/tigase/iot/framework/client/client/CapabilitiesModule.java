/*
 * CapabilitiesModule.java
 *
 * Tigase IoT Framework
 * Copyright (C) 2011-2018 "Tigase, Inc." <office@tigase.com>
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client;

import tigase.jaxmpp.core.client.*;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ContextAware;
import tigase.jaxmpp.core.client.xmpp.modules.InitializingModule;
import tigase.jaxmpp.core.client.xmpp.modules.SoftwareVersionModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.disco.NodeDetailsCallback;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.BeforePresenceSendHandler;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.logging.Logger;

public class CapabilitiesModule implements XmppModule, ContextAware, InitializingModule {

	public final static String NODE_NAME_KEY = "NODE_NAME_KEY";
	public static final String VERIFICATION_STRING_KEY = "XEP115VerificationString";
	private final static String ALGORITHM = "SHA-1";
	private final Logger log = Logger.getLogger(this.getClass().getName());
	//private CapabilitiesCache cache;
	private Context context;
	private DiscoveryModule discoveryModule;
	private NodeDetailsCallback nodeDetailsCallback;

	// private final XmppModulesManager modulesManager;

	public CapabilitiesModule() {
	}

	@Override
	public void afterRegister() {
	}

	@Override
	public void beforeRegister() {
		if (context == null)
			throw new RuntimeException("Context cannot be null");

		this.discoveryModule = context.getModuleProvider().getModule(DiscoveryModule.class);
		if (this.discoveryModule == null)
			throw new RuntimeException("Required module: DiscoveryModule not available.");

		PresenceModule presenceModule = context.getModuleProvider().getModule(PresenceModule.class);
		if (presenceModule == null)
			throw new RuntimeException("Required module: PresenceModule not available.");

		final BeforePresenceSendHandler beforePresenceSendHandler = new BeforePresenceSendHandler() {

			@Override
			public void onBeforePresenceSend(SessionObject sessionObject, Presence presence) throws JaxmppException {
				CapabilitiesModule.this.onBeforePresenceSend(presence);
			}

		};
		nodeDetailsCallback = new DiscoveryModule.DefaultNodeDetailsCallback(discoveryModule);

		presenceModule.addBeforePresenceSendHandler(beforePresenceSendHandler);

		discoveryModule.setNodeCallback("", nodeDetailsCallback);

	}

	@Override
	public void beforeUnregister() {
	}

	private String calculateVerificationString() {
		String category = context.getSessionObject().getProperty(DiscoveryModule.IDENTITY_CATEGORY_KEY);
		String type = context.getSessionObject().getProperty(DiscoveryModule.IDENTITY_TYPE_KEY);
		String nme = context.getSessionObject().getProperty(SoftwareVersionModule.NAME_KEY);
		String v = context.getSessionObject().getProperty(SoftwareVersionModule.VERSION_KEY);

		String identity = category + "/" + type + "//" + nme + " " + v;

		String ver = generateVerificationString(new String[] { identity },
				context.getModuleProvider().getAvailableFeatures().toArray(new String[] {}));

		String oldVer = context.getSessionObject().getProperty(VERIFICATION_STRING_KEY);
		if (oldVer != null && !oldVer.equals(ver)) {
			discoveryModule.removeNodeCallback(getNodeName() + "#" + oldVer);
		}

		context.getSessionObject().setProperty(VERIFICATION_STRING_KEY, ver);
		discoveryModule.setNodeCallback(getNodeName() + "#" + ver, nodeDetailsCallback);

		return ver;
	}

	public final String generateVerificationString(String[] identities, String[] features) {
		try {
			com.googlecode.gwt.crypto.bouncycastle.digests.SHA1Digest md = new com.googlecode.gwt.crypto.bouncycastle.digests.SHA1Digest();

			for (String id : identities) {
				byte[] tmp = id.getBytes("UTF-8");
				md.update(tmp, 0, tmp.length);
				md.update((byte) '<');
			}

			Arrays.sort(features);

			for (String f : features) {
				byte[] tmp = f.getBytes("UTF-8");
				md.update(tmp, 0, tmp.length);
				md.update((byte) '<');
			}

			byte[] digest = new byte[md.getDigestSize()];
			md.doFinal(digest, 0);
			return Base64.encode(digest);
		} catch (UnsupportedEncodingException e) {
			log.warning("Cannot calculate verification string.");
		}
		return null;
	}

	@Override
	public Criteria getCriteria() {
		return null;
	}

	@Override
	public String[] getFeatures() {
		return new String[] { "http://jabber.org/protocol/caps" };
	}

	protected String getNodeName() {
		String s = context.getSessionObject().getProperty(NODE_NAME_KEY);
		return s == null ? "http://tigase.org/jaxmpp" : s;
	}

	protected boolean isEnabled() {
		return true;
	}

	protected void onBeforePresenceSend(final Presence presence) throws XMLException {
		if (!isEnabled())
			return;
		String ver = context.getSessionObject().getProperty(VERIFICATION_STRING_KEY);
		if (ver == null) {
			ver = calculateVerificationString();
		}
		if (ver == null)
			return;

		if (presence != null) {
			synchronized (presence) {
				final Element c = ElementFactory.create("c", null, "http://jabber.org/protocol/caps");
				c.setAttribute("hash", "sha-1");
				c.setAttribute("node", getNodeName());
				c.setAttribute("ver", ver);
				presence.addChild(c);
			}
		}
	}

	@Override
	public void process(Element element) throws XMPPException, XMLException, JaxmppException {
	}

	@Override
	public void setContext(Context context) {
		this.context = context;
	}
}
