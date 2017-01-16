##  XMPP connections
#xmppService () {
#    default () {
#        jid = "admin@zeus"
#        password = "admin"
#        server = '172.16.0.2'
#        ignoreCertificateErrors = true
#    }
#}

## IoT Devices

### Light dimmer
#lightsDimmer (class: 'tigase.rpi.home.app.devices.ReactiveLightDimmer', exportable: true) {
#    pathToTransmitRF = '/home/pi/TransmitRF.py'
#    requirePir = true
#    lightSensorPubSubNodes = [ 'devices/lightSensor/state' ];
#    pirSensorPubSubNodes = [ 'devices/tvSensor/state', 'devices/pirSensor/state' ]
#    turnOnIfLowerThan = 30
#    turnOffIfHigherThan = 40
#}

### Light sensor
#lightSensor (class: 'tigase.rpi.home.sensors.light.BH1750', exportable: true) {
#    # decimal address of BH1750 on I2C
#    address = 35
#}

### Movement sensor
#pirSensor (class: 'tigase.rpi.home.sensors.pir.HC_SR501', exportable: true) {
#
#}

### Android TV sensor
#tvSensor (class: 'tigase.rpi.home.sensors.pir.AndroidTv', exportable: true) {
#    # HTTP URI of Android TV system endpoint
#    address = 'http://172.16.0.224/sony/system'
#}

### Device represents master - loads every attached and known to application 1Wire device
w1Master (class: 'tigase.rpi.home.sensors.w1.W1Master', exportable: true) {
    # How often to look for new 1Wire devices (in ms)
    period = 15000
}

### Configuration of temperature readout from W1-DS1820 with ID = 28-0000084a4859
#w1-28-0000084a4859 {
#    period = 15000
#}

##  Modules

### Presence publisher for XMPP
presencePublisherDemo (class: 'tigase.rpi.home.app.PresencePublisherDemo', exportable: true) {
}

### Publishes device and sensors state to PubSub
devicePubSubPublisher (class: 'tigase.rpi.home.app.pubsub.DevicePubSubPublisher', exportable: true) {
}

### Manages PubSub node creation for devices, etc.
pubSubNodesManager (class: 'tigase.rpi.home.app.pubsub.ExtendedPubSubNodesManager', exportable: true) {
    PEP = true
}

### Stores device configuration in PubSub nodes and restores devices configuration from PubSub nodes
deviceConfigManager (class: 'tigase.rpi.home.app.DeviceConfigurationPubSubManager', exportable: true) {
}

