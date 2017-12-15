/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.ui;

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
	
	public TopBar(String name) {
		panel = new FlowPanel();
		panel.setStylePrimaryName("top-bar");
		title = new Label(name);
		panel.add(title);
		
		initWidget(panel);
	}
	
	public void setTitle(String title) {
		this.title.setText(title);
	}
	
	public Label addAction(String name, ClickHandler action) {
		Label close = new Label(name);
		close.setStylePrimaryName("top-bar-action");
		panel.add(close);
		if (action != null) {
			close.addClickHandler(action);
		}
		return close;
	}
	
}
