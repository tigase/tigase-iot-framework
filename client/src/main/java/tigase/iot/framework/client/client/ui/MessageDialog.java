/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import java.util.List;
import tigase.jaxmpp.core.client.xml.Element;

/**
 *
 * @author andrzej
 */
public class MessageDialog {

	private final DialogBox errorDlg;

	public MessageDialog(String title, String message) {
		this(title, message, null);
	}
	
	public MessageDialog(String title, String message, Runnable onOK) {
		errorDlg = new DialogBox(true, true);
		errorDlg.setStylePrimaryName("dialog-window");
		errorDlg.setGlassEnabled(true);
		errorDlg.setTitle(title);
		errorDlg.setText(message);
		Button button = new Button("OK");
		button.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				errorDlg.hide();
				if (onOK != null) {
					onOK.run();
				}
			}
		});
		errorDlg.setWidget(button);
		button.getElement().getParentElement().addClassName("context-menu");
	}

	public void show() {
		errorDlg.center();
	}
}
