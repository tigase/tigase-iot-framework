/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.rpi.home.client.client.ui;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

/**
 *
 * @author andrzej
 */
public class TopBar extends Composite {
	
	private final FlowPanel panel;
	private final Label title;
	
	public TopBar(String name, ClickHandler closeAction) {
		panel = new FlowPanel();
		panel.setStylePrimaryName("top-bar");
		title = new Label(name);
		panel.add(title);
		
		if (closeAction != null) {
			Label close = new Label("\u2716");
			close.setStylePrimaryName("close-action");
			panel.add(close);
			close.addClickHandler(closeAction);
		}
		
		initWidget(panel);
	}
	
	public void setTitle(String title) {
		this.title.setText(title);
	}
	
}
