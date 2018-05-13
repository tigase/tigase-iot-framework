/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;

/**
 *
 * @author andrzej
 */
public class MessageDialog {

	private final DialogBox errorDlg;
	private Runnable onCancel;

	public MessageDialog(String title, String message) {
		this(title, message, null);
	}
	
	public MessageDialog(String title, SafeHtml message) {
		this(title, message, null);
	}

	public MessageDialog(String title, String message, Runnable onOK) {
		errorDlg = new DialogBox(true, true) {
			@Override
			public void hide(boolean autoClosed) {
				super.hide(autoClosed);
				if (onCancel != null && autoClosed) {
					onCancel.run();
				}
			}
		};
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

	public MessageDialog(String title, SafeHtml message, Runnable onOK) {
		errorDlg = new DialogBox(true, true);
		errorDlg.setStylePrimaryName("dialog-window");
		errorDlg.setGlassEnabled(true);
		errorDlg.setTitle(title);
		errorDlg.setHTML(SafeHtmlUtils.fromSafeConstant("<h2>" + SafeHtmlUtils.fromString(title).asString() + "</h2><br/>" + message.asString()));
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

	public MessageDialog onCancel(Runnable run) {
		onCancel = run;
		return this;
	}
}
