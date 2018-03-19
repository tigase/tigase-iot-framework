/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import tigase.iot.framework.client.Device;
import tigase.iot.framework.client.Devices;
import tigase.iot.framework.client.Hub;
import tigase.iot.framework.client.Hub.RemoteConnectionStatusCallback;
import tigase.iot.framework.client.client.ActiveHostsChangedEvent;
import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.client.FlexGrid;
import tigase.iot.framework.client.client.ui.MessageDialog;
import tigase.iot.framework.client.client.ui.TopBar;
import tigase.iot.framework.client.devices.TemperatureSensor;
import tigase.iot.framework.client.modules.SubscriptionModule;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author andrzej
 */
public class DevicesListViewImpl extends Composite implements DevicesListView {

	private final ClientFactory factory;

	private final FlexGrid flexGrid;

	private final Label hostsLabel;
	
	public DevicesListViewImpl(ClientFactory factory) {
		this.factory = factory;

		this.factory.jaxmpp().getEventBus().addHandler(Devices.ChangedHandler.ChangedEvent.class, new Devices.ChangedHandler() {
			@Override
			public void devicesChanged(List<Device> devices) {
				updateDevices(devices);
			}
		});

		//AbsolutePanel panel = new AbsolutePanel();
		DockLayoutPanel panel = new DockLayoutPanel(Style.Unit.EM);

		TopBar topBar = new TopBar("Devices");
		
		topBar.addAction("\u2716", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				try {
					factory.jaxmpp().disconnect();
				} catch (JaxmppException ex) {
					Logger.getLogger(DevicesListViewImpl.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});

		topBar.addAction("\uD83D\uDD11", new ClickHandler() {
			@Override
			public void onClick(ClickEvent clickEvent) {
				showAllDevicesList();
			}
		});
		
		topBar.addAction("\uD83D\uDCF6", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				FlowPanel panel = new FlowPanel();
				panel.setStylePrimaryName("context-menu");

				DialogBox dialog = new DialogBox(true, true);
				dialog.setStylePrimaryName("dialog-window");
				dialog.setTitle("Add device");

				Label connectionStatus = new Label("Connection status");
				connectionStatus.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						try {
							factory.hub().checkRemoteConnectionStatus(new Hub.RemoteConnectionStatusCallback() {
								@Override
								public void onResult(RemoteConnectionStatusCallback.State state, Integer retry, XMPPException.ErrorCondition errorCondition) {
									if (errorCondition != null) {
										new MessageDialog("Error", "Server returned an error: " + errorCondition.name()).show();
										return;
									}
									String name = "Unknown";
									switch (state) {
										case awaitReconnection:
											name = "Awaiting for reconnection (try: " + retry + ")";
											break;
										case reconnecting:
											name = "Reconnecting (try: " + retry + ")";
											break;
										case connected:
											name = "Connected";
											break;
									}
									
									new MessageDialog("Status", SafeHtmlUtils.fromSafeConstant("Connection: " + name)).show();
								}
							});
						} catch (JaxmppException ex) {
							Logger.getLogger(DevicesListViewImpl.class.getName()).log(Level.SEVERE, null, ex);
						}
						dialog.hide();
					}					
				});
				panel.add(connectionStatus);
				
				Label forceReconnection = new Label("Force reconnection");
				forceReconnection.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						try {
							factory.hub().forceRemoteConnectionReconnection(new Hub.RemoteConnectionReconnectionCallback() {
								@Override
								public void onResult(XMPPException.ErrorCondition errorCondition) {
									if (errorCondition != null) {
										new MessageDialog("Error", "Server returned an error: " + errorCondition.name()).show();
										return;
									}
									new MessageDialog("Success", "Reconnection initiated").show();
								}
							});
						} catch (JaxmppException ex) {
							Logger.getLogger(DevicesListViewImpl.class.getName()).log(Level.SEVERE, null, ex);
						}
						dialog.hide();
					}
				});
				panel.add(forceReconnection);
				
				Label remoteConnectionCredentials = new Label("Connection credentials");
				remoteConnectionCredentials.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						try {
						factory.hub().getRemoteConnectionCredentials(new Hub.RemoteConnectionCredentialsCallback() {
								@Override
								public void onResult(String username, String password, XMPPException.ErrorCondition errorCondition) {
									if (errorCondition != null) {
										new MessageDialog("Error", "Server returned an error: " + errorCondition.name()).show();
										return;
									}

									new MessageDialog("Credentials", SafeHtmlUtils.fromSafeConstant("Username: " + username + "<br/>Password: " + password)).show();
								}
							});
						} catch (JaxmppException ex) {
							Logger.getLogger(DevicesListViewImpl.class.getName()).log(Level.SEVERE, null, ex);
						}
						dialog.hide();
					}
				});
				panel.add(remoteConnectionCredentials);

				Label subscriptionStatus = new Label("Subscription");
				subscriptionStatus.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent clickEvent) {
						dialog.hide();

						SubscriptionModule.Subscription sub = factory.hub().getSubscription();
						String txt = "";
						if (sub == null) {
							txt = "Subscription informations not available.<br/>Please try again later.";
						} else {
							txt = "Subscription allows for:<br/><ul>";
							txt += "<li>";
							if (sub.devices < 0) {
								txt += "Usage of any number of devices";
							} else {
								txt += "Usage of " + sub.devices + " devices";
							}
							txt += "</li><li>";
							if (sub.changesPerSecond < 0) {
								txt += "Unlimited changes per second";
							} else {
								txt += "" + String.valueOf(sub.changesPerSecond) + " changes per second";
							}
							txt += "</li></ul>";
						}
						new MessageDialog("Subscription", SafeHtmlUtils.fromSafeConstant(txt)).show();
					}
				});
				panel.add(subscriptionStatus);

				dialog.setWidget(panel);
				dialog.center();
			}
		});
		
		topBar.addAction("\uD83D\uDD04", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				try {
					factory.devices().refreshDevices();
				} catch (JaxmppException ex) {
					Logger.getLogger(DevicesListViewImpl.class.getName()).log(Level.SEVERE, null, ex);
				}
			}			
		});
		
		hostsLabel = topBar.addAction("Hosts: 0", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				factory.hosts().getActiveDeviceHosts(new Devices.DevicesInfoRetrieved() {
					@Override
					public void onDeviceInfoRetrieved(Map<JID, DiscoveryModule.Identity> activeHosts) {
						List<Map.Entry<JID, DiscoveryModule.Identity>> items = new ArrayList<>(activeHosts.entrySet());
						items.sort(new Comparator<Map.Entry<JID, DiscoveryModule.Identity>>() {
							@Override
							public int compare(Map.Entry<JID, DiscoveryModule.Identity> o1, Map.Entry<JID, DiscoveryModule.Identity> o2) {
								return o1.getValue().getName().compareTo(o2.getValue().getName());
							}
						});
			
						StringBuilder sb = new StringBuilder();
						for (Map.Entry<JID, DiscoveryModule.Identity> e : items) {
							if (sb.length() > 0) {
								sb.append("<br/>");
							}
							sb.append(e.getValue().getName());
						}
						new MessageDialog("Connected hosts: " + activeHosts.size(), SafeHtmlUtils.fromSafeConstant(sb.toString())).show();
					}
				});
			}			
		});
		factory.eventBus().addHandler(ActiveHostsChangedEvent.TYPE, new ActiveHostsChangedEvent.Handler() {
			@Override
			public void onActiveHostsChange(Map<JID, DiscoveryModule.Identity> activeHosts) {
				hostsLabel.setText("Hosts: " + activeHosts.size());
			}
		});

		panel.addNorth(topBar, 2.2);

		flexGrid = new FlexGrid();

		panel.add(new ScrollPanel(flexGrid));

		initWidget(panel);
	}

	protected void updateDevices(List<Device> devices) {
		flexGrid.clear();
		
		devices.sort(new Comparator<Device>(){
			@Override
			public int compare(Device o1, Device o2) {
				return (o1.getName() != null ? o1.getName() : "").compareTo(o2.getName() != null ? o2.getName() : "");
			}			
		});

		for (Device device : devices) {
			if (device instanceof TemperatureSensor) {
				Thermometer item = new Thermometer(factory, ((TemperatureSensor) device));
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.Switch) {
				Switch item = new Switch(factory, (tigase.iot.framework.client.devices.Switch) device);
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.LightDimmer) {
				LightsDimmer item = new LightsDimmer(factory, (tigase.iot.framework.client.devices.LightDimmer) device);
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.LightSensor) {
				LightSensor item = new LightSensor(factory, (tigase.iot.framework.client.devices.LightSensor) device);
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.TvSensor) {
				TvIndicator item = new TvIndicator(factory, (tigase.iot.framework.client.devices.TvSensor) device);
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.MovementSensor) {
				MovementSensor item = new MovementSensor(factory, (tigase.iot.framework.client.devices.MovementSensor) device);
				flexGrid.add(item);
			} else if (device instanceof tigase.iot.framework.client.devices.HumiditySensor) {
				HumiditySensor item = new HumiditySensor(factory, (tigase.iot.framework.client.devices.HumiditySensor) device);
				flexGrid.add(item);
			}
		}

		flexGrid.add(new AddDeviceItem());
	}

	private void showAllDevicesList() {
		try {
			factory.hub().retrieveAccounts(new Hub.RetrieveAccountsCallback() {
				@Override
				public void onResult(List<Result> results, XMPPException.ErrorCondition error) {
					factory.hosts().getActiveDeviceHosts(new Devices.DevicesInfoRetrieved() {
						@Override
						public void onDeviceInfoRetrieved(Map<JID, DiscoveryModule.Identity> devicesInfo) {
							Grid grid = new Grid(results.size() + 1, 3);
							grid.setHTML(0, 0, "<b>Status</b>");
							grid.setHTML(0, 1, "<b>Device ID</b>");
							grid.setHTML(0, 2, "<b>Action</b>");

							grid.getElement().getStyle().setWidth(100, Style.Unit.PCT);

							DialogBox dialog = new DialogBox(true, true);
							dialog.setStylePrimaryName("dialog-window");
							dialog.setTitle("Manage devices");
							dialog.getCaption().setHTML(SafeHtmlUtils.fromSafeConstant("<h3 style='margin-top: 0px;'>Manage devices</h3>"));

							int row = 0;
							grid.getCellFormatter().setWidth(row, 0, "60px");
							grid.getCellFormatter().setWidth(row, 2, "60px");
							row++;
							for (Result result : results) {
								Label label = new Label(result.jid.getLocalpart());
								grid.setWidget(row, 1, label);

								//DiscoveryModule.Identity identity = devicesInfo.get(result.jid);
								try {
									Presence p = PresenceModule.getPresenceStore(factory.jaxmpp().getSessionObject()).getBestPresence(result.jid.getBareJid());
									if (p == null || p.getType() == StanzaType.unavailable) {
										grid.setText(row, 0, "\u2601");
									} else {
										grid.setText(row, 0, "\u2600");
									}
								} catch (Exception ex) {}

								Label action = null;
								Result.ActionCallback handler = newActionHandler();
								switch (result.status) {
									case active:
										action = new Label("\u274E");
										action.addClickHandler(new ClickHandler() {
											@Override
											public void onClick(ClickEvent clickEvent) {
												dialog.hide(false);
												result.disable(handler);
											}
										});
										grid.setText(row, 2, "\u274E");
										break;
									case disabled:
										action = new Label("\u2705");
										action.addClickHandler(new ClickHandler() {
											@Override
											public void onClick(ClickEvent clickEvent) {
												dialog.hide(false);
												result.enable(handler);
											}
										});

										grid.setText(row, 2, "\u2705");
										break;
									case pending:
										action = new Label("\u2705");
										action.addClickHandler(new ClickHandler() {
											@Override
											public void onClick(ClickEvent clickEvent) {
												dialog.hide(false);
												result.enable(handler);
											}
										});
										grid.setText(row, 0, "\uD83C\uDF1F");
										break;
								}
								grid.setWidget(row, 2, action);
								row++;
							}


							dialog.setWidget(grid);
							dialog.getElement().getFirstChildElement().getFirstChildElement().getStyle().setWidth(100, Style.Unit.PCT);
							dialog.center();
						}
					});
				}
			});
		} catch (JaxmppException ex) {
			Logger.getLogger(DevicesListViewImpl.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private Hub.RetrieveAccountsCallback.Result.ActionCallback newActionHandler() {
		return new Hub.RetrieveAccountsCallback.Result.ActionCallback() {
			@Override
			public void onError(XMPPException.ErrorCondition errorCondition, String errorMessage) {
				new MessageDialog("Error", SafeHtmlUtils.fromSafeConstant(errorMessage), new Runnable() {
					@Override
					public void run() {
						showAllDevicesList();
					}
				}).show();
			}

			@Override
			public void onSuccess() {
				showAllDevicesList();
			}
		};
	}

	public class AddDeviceItem extends Composite {

		private final Label icon;
		private final Label label;

		public AddDeviceItem() {
			FlowPanel item = new FlowPanel();
			item.setStylePrimaryName("flex-device-item");
			item.addStyleName("add-device");

			icon = new Label("\u2795");
			icon.setStylePrimaryName("icon");
			icon.getElement().getStyle().setFloat(Style.Float.NONE);

			item.add(icon);

			label = new Label();
			label.setStylePrimaryName("label");
			label.setText("Add device");
			item.add(label);

			initWidget(item);

			Widget w = asWidget();
			w.sinkEvents(Event.ONCLICK);
			w.addHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					clicked();
				}
			}, ClickEvent.getType());

		}

		public void clicked() {
			try {
				factory.devices().getActiveDeviceHosts(new Devices.DevicesInfoRetrieved() {
					@Override
					public void onDeviceInfoRetrieved(Map<JID, DiscoveryModule.Identity> devicesInfo) {
						if (devicesInfo.isEmpty()) {
							new MessageDialog("Error", "Could not found any IoT hosts.\nPlease check if IoT hosts are turned on.").show();
						} else {
							showContextMenu(devicesInfo);
						}
					}
				});
			} catch (JaxmppException ex) {
				Logger.getLogger(DevicesListViewImpl.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		protected void showContextMenu(Map<JID, DiscoveryModule.Identity> devicesInfo) {
			final DialogBox dialog = new DialogBox(true, true);
			dialog.setStylePrimaryName("dialog-window");

			FlowPanel panel = new FlowPanel();
			panel.setStylePrimaryName("context-menu");
			
			List<Map.Entry<JID, DiscoveryModule.Identity>> items = new ArrayList<>(devicesInfo.entrySet());
			items.sort(new Comparator<Map.Entry<JID, DiscoveryModule.Identity>>() {
				@Override
				public int compare(Map.Entry<JID, DiscoveryModule.Identity> o1, Map.Entry<JID, DiscoveryModule.Identity> o2) {
					return o1.getValue().getName().compareTo(o2.getValue().getName());
				}
			});
			
			for (Map.Entry<JID, DiscoveryModule.Identity> item : items) {
				Label itemLabel = new Label(item.getValue().getName());
				itemLabel.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						//Window.alert("select " + item.getValue().getName() + " at " + item.getKey());
						dialog.hide();
						new AddDeviceDlg(factory, item.getKey());
					}
				});
				panel.add(itemLabel);
			}

			dialog.setWidget(panel);
			dialog.center();

		}
	}

}
