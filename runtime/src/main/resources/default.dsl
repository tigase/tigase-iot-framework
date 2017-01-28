##  XMPP connections
#xmppService () {
#    default () {
#        jid = "admin@zeus"
#        password = "admin"
#        server = '172.16.0.2'
#        ignoreCertificateErrors = true
#    }
#}

##  Modules

adHocCommands(class: 'tigase.bot.runtime.AdHocCommandsBridge', exportable: true){
}

commandsManager(class: 'tigase.bot.runtime.messages.CommandsManager', exportable: true){
}

messagesBridge(class: 'tigase.bot.runtime.messages.XmppMessagesBridge', exportable: true){
}

groovyShell(class: 'tigase.bot.runtime.messages.GroovyConsoleManager', exportable: true){
}

configCommand(class: 'tigase.bot.runtime.BeanConfigureAdHocCommand', exportable: true){
}

### Presence publisher for XMPP
presencePublisher(class: 'tigase.bot.runtime.PresencePublisher', exportable: true){
}

### Publishes device and sensors state to PubSub
devicePubSubPublisher (class: 'tigase.iot.framework.runtime.pubsub.DevicePubSubPublisher', exportable: true) {
}

### Manages PubSub node creation for devices, etc.
pubSubNodesManager (class: 'tigase.iot.framework.runtime.pubsub.ExtendedPubSubNodesManager', exportable: true) {
#    PEP = true
}

### Stores device configuration in PubSub nodes and restores devices configuration from PubSub nodes
deviceConfigManager (class: 'tigase.iot.framework.runtime.DeviceConfigurationPubSubManager', exportable: true) {
}

## IoT Devices

### Light sensor
#lightSensor (class: 'tigase.iot.framework.sensors.light.BH1750', exportable: true) {
#    # decimal address of BH1750 on I2C
#    address = 35
#}

### Movement sensor
#pirSensor (class: 'tigase.iot.framework.sensors.pir.HC_SR501', exportable: true) {
#
#}

### Android TV sensor
#tvSensor (class: 'tigase.iot.framework.sensors.pir.AndroidTv', exportable: true) {
#    # HTTP URI of Android TV system endpoint
#    address = 'http://172.16.0.224/sony/system'
#}

### Device represents master - loads every attached and known to application 1Wire device
#w1Master (class: 'tigase.iot.framework.sensors.w1.W1Master', exportable: true) {
#    # How often to look for new 1Wire devices (in ms)
#    period = 15000
#}

### Configuration of temperature readout from W1-DS1820 with ID = 28-0000084a4859
#w1-28-0000084a4859 {
#    period = 15000
#}
