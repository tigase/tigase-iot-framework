/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import tigase.iot.framework.client.client.ClientFactory;

/**
 *
 * @author andrzej
 */
public class Switch extends AbstractSwitch {

	public Switch(ClientFactory factory, tigase.iot.framework.client.devices.Switch sensor) {
		super(factory, "switch", Icons.INSTANCE.shutdown(), sensor);
	}

}
