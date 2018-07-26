/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.iot.framework.client.client.devices;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import tigase.iot.framework.client.Device;
import tigase.iot.framework.client.Devices;
import tigase.iot.framework.client.Hub;
import tigase.iot.framework.client.Hub.RemoteConnectionStatusCallback;
import tigase.iot.framework.client.client.ActiveHostsChangedEvent;
import tigase.iot.framework.client.client.ClientFactory;
import tigase.iot.framework.client.client.CloudActivationEmailDialog;
import tigase.iot.framework.client.client.FlexGrid;
import tigase.iot.framework.client.client.ui.MessageDialog;
import tigase.iot.framework.client.client.ui.TopBar;
import tigase.iot.framework.client.devices.TemperatureSensor;
import tigase.iot.framework.client.modules.SubscriptionModule;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.SessionObject;
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

	private final RegExp UUID_REGEX = RegExp.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

	private final ClientFactory factory;

	private final FlexGrid flexGrid;

	private final Label changesPerMinuteLabel;
	private final Label totalDevicesLabel;
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

				Hub.CloudSettings settings = factory.hub().getCloudSettings();
				if ("tigase-iot-hub.local".equals(factory.jaxmpp().getSessionObject().getUserBareJid().getDomain())) {
					if (settings != null) {
						ToggleButton toggleButton = new ToggleButton("Enable IoT One Cloud", "Disable IoT One Cloud");
						toggleButton.addClickHandler(new ClickHandler() {
							@Override
							public void onClick(ClickEvent clickEvent) {
								if (toggleButton.isDown() && (settings.email == null || settings.email.isEmpty())) {
									dialog.hide();
									new CloudActivationEmailDialog(factory).show();
								} else {
									dialog.hide();
									try {
										factory.hub().updateCloudSettings(toggleButton.isDown(), settings.email, new Hub.CompletionHandler() {
											@Override
											public void onResult(XMPPException.ErrorCondition errorCondition) {
												if (errorCondition != null) {
													toggleButton.setDown(!toggleButton.isDown());
													new MessageDialog("Error",
																	  "Could not update settings. Please try again later.")
															.show();
												}
											}
										});
									} catch (JaxmppException ex) {
										toggleButton.setDown(false);
										new MessageDialog("Error",
														  "Could not update settings. Please try again later.")
												.show();
									}
								}
							}
						});
						toggleButton.setDown(settings.enabled);
						panel.add(toggleButton);
						toggleButton.getElement().getStyle().setOutlineWidth(0.0, Style.Unit.PX);
					}
				}

				if (settings == null || settings.enabled) {
					Label connectionStatus = new Label("Connection status");
					connectionStatus.addClickHandler(new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							try {
								factory.hub().checkRemoteConnectionStatus(new Hub.RemoteConnectionStatusCallback() {
									@Override
									public void onResult(RemoteConnectionStatusCallback.State state, Integer retry,
														 XMPPException.ErrorCondition errorCondition) {
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
									public void onResult(String username, String password, String domain, XMPPException.ErrorCondition errorCondition) {
										if (errorCondition != null) {
											new MessageDialog("Error", "Server returned an error: " + errorCondition.name()).show();
											return;
										}

										new MessageDialog("Credentials", SafeHtmlUtils.fromSafeConstant(
												"Username: " + username + "<br/>Password: " + password)).show();
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
							if (sub == null) {
								String txt = "Subscription information not available.<br/>Please try again later.";
								new MessageDialog("Subscription", SafeHtmlUtils.fromSafeConstant(txt)).show();
							} else {
								try {
									factory.hub().retrieveHubSubscriptionUsage(new Hub.CloudStatiscsCallback() {
										@Override
										public void onResult(Hub.CloudStatistics statistics, XMPPException.ErrorCondition errorCondition,
															 String message) throws JaxmppException {
											String txt = txt = "Subscription allows for:<br/><ul>";
											txt += "<li>";
											if (sub.devices < 0) {
												txt += "Usage of any number of devices";
											} else {
												txt += "Usage of " + sub.devices + " devices";
											}
											txt += "</li><li>";
											if (sub.changesPerMinute < 0) {
												txt += "Unlimited changes per minute";
											} else {
												txt += "" + String.valueOf((int) sub.changesPerMinute) +
														" changes per minute";
											}
											txt += "</li></ul>";

											if (statistics != null) {
												txt += "<br/>";
												txt += "You have " + statistics.devices + " devices";
												SubscriptionModule.SubscriptionUsage usage = factory.hub().getSubscriptionUsage();
												if (usage != null) {
													txt += "and you are sending " + String.valueOf((int) usage.changesPerMinute) + " requests per minute";
												}
												txt += ".";

												if (statistics.queuedChanges > 0) {
													txt += "<br/>";
													txt += String.valueOf(statistics.queuedChanges) + " changes waiting for delivery to the IoT1 Cloud!";
												}
											}
											new MessageDialog("Subscription", SafeHtmlUtils.fromSafeConstant(txt)).show();
										}
									});
								} catch (JaxmppException ex) {
									String txt = "Subscription information not available.<br/>Please try again later.";
									new MessageDialog("Subscription", SafeHtmlUtils.fromSafeConstant(txt)).show();
								}
							}
						}
					});
					panel.add(subscriptionStatus);
				}

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

		changesPerMinuteLabel = topBar.addAction("\u23F1 0 req/min", new ClickHandler() {
			@Override
			public void onClick(ClickEvent clickEvent) {
				// nothing to do...
			}
		});
		factory.jaxmpp()
				.getEventBus()
				.addHandler(SubscriptionModule.SubscriptionUsageChangedHandler.SubscriptionUsageChangedEvent.class,
							new SubscriptionModule.SubscriptionUsageChangedHandler() {

								@Override
								public void subscriptionUsageChanged(SessionObject sessionObject,
																	 SubscriptionModule.SubscriptionUsage subscriptionUsage) {
									changesPerMinuteLabel.setText("\u23F1 " + subscriptionUsage.changesPerMinute + " req/min");
								}
							});

		totalDevicesLabel = topBar.addAction("", new ClickHandler() {
			@Override
			public void onClick(ClickEvent clickEvent) {
				// nothing to do...
			}
		});
		factory.jaxmpp().getEventBus().addHandler(Devices.TotalDevicesCountChangedHandler.TotalDevicesCountChangedEvent.class,
												  new Devices.TotalDevicesCountChangedHandler() {
													  @Override
													  public void totalDevicesCountChanged(Integer totalDevicesCount) {
														  totalDevicesLabel.setText(totalDevicesCount == null ? "" : ((totalDevicesCount == 1) ? "\ud83d\udce1 1 device" : ("\ud83d\udce1 " + totalDevicesCount + " devices")));
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
			} else if (device instanceof tigase.iot.framework.client.devices.PressureSensor) {
				PressureSensor item = new PressureSensor(factory, (tigase.iot.framework.client.devices.PressureSensor) device);
				flexGrid.add(item);
			} else {
				UnknownDevice item = new UnknownDevice(factory, device);
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
							FlexTable grid = new FlexTable();//results.size() + 1, 3);
							grid.setHTML(0, 0, "<b>Status</b>");
							grid.setHTML(0, 1, "<b>Device ID</b>");
							grid.setHTML(0, 2, "<b>Action</b>");
							grid.getFlexCellFormatter().setColSpan(0, 2, 2);

							grid.getElement().getStyle().setWidth(100, Style.Unit.PCT);

							DialogBox dialog = new DialogBox(true, true);
							dialog.setStylePrimaryName("dialog-window");
							dialog.setTitle("Manage devices");
							dialog.getCaption().setHTML(SafeHtmlUtils.fromSafeConstant("<h3 style='margin-top: 0px;'>Manage devices</h3>"));

							int row = 0;
							grid.getCellFormatter().setWidth(row, 0, "60px");
							grid.getCellFormatter().setWidth(row, 2, "30px");
							grid.getCellFormatter().setWidth(row, 3, "30px");
							row++;
							for (Result result : results) {
								DiscoveryModule.Identity identity = null;

								try {
									Presence p = PresenceModule.getPresenceStore(factory.jaxmpp().getSessionObject()).getBestPresence(result.jid.getBareJid());
									Label presenceLabel = null;
									if (p == null || p.getType() == StanzaType.unavailable) {
										presenceLabel = new Label("\u2601");
									} else {
										presenceLabel = new Label("\u2600");
									}
									presenceLabel.getElement().getStyle().setFontSize(1.6, Style.Unit.EM);
									grid.setWidget(row, 0, presenceLabel);
									if (p != null && p.getFrom() != null) {
										identity = devicesInfo.get(p.getFrom());
									}
								} catch (Exception ex) {}

								boolean isClient = UUID_REGEX.exec(result.jid.getLocalpart()) == null;

								String title = isClient ? result.jid.getLocalpart() : (result.name != null ? result.name : "Unknown");
								String subtitle = isClient ? "User client" : (result.os != null ? result.os : result.jid.getLocalpart());

								SafeHtmlBuilder sb = new SafeHtmlBuilder();
								sb.append(SafeHtmlUtils.fromString(title));
								sb.appendHtmlConstant("<br/>");
								sb.appendHtmlConstant("<span style='font-size: 0.75em'>");
								sb.append(SafeHtmlUtils.fromString(subtitle));
								sb.appendHtmlConstant("</span>");
								HTML label = new HTML(sb.toSafeHtml());
								grid.setWidget(row, 1, label);

								Button action = null;
								Result.ActionCallback handler = newActionHandler();
								switch (result.status) {
									case active:
										action = createButton("\u274E", "Disable");
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
										action = createButton("\u2705", "Enable");
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
										action = createButton("\u2705", "Enable");
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

								action = createButton("\u274c", "Remove");
								action.addClickHandler(new ClickHandler() {
									@Override
									public void onClick(ClickEvent clickEvent) {
										dialog.hide(false);
										new MessageDialog("Account removal", SafeHtmlUtils.fromSafeConstant(
												"You are about to delete the account for " + result.jid.getLocalpart() +
														".<br/>Are you sure?"), new Runnable() {
											@Override
											public void run() {
												result.delete(handler);
											}
										}).show();
									}
								});
								grid.setWidget(row, 3, action);
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

	private Button createButton(String label, String title) {
		Button actionb = new Button(label);
		actionb.setStyleName(null);
		actionb.getElement().getStyle().setBackgroundColor("transparent");
		actionb.getElement().getStyle().setBorderWidth(0.0, Style.Unit.PX);
		actionb.setTitle(title);
		return actionb;
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
