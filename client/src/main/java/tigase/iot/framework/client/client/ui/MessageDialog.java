/*
 * MessageDialog.java
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
