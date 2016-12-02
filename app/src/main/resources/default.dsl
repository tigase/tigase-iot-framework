xmppService () {
    default () {
        jid = "admin@zeus"
        password = "admin"
        server = '172.16.0.2'
        ignoreCertificateErrors = true
    }
}

presencePublisherDemo (class: 'tigase.rpi.home.app.PresencePublisherDemo', exportable: true) {

}

lightsDimmer (class: 'tigase.rpi.home.app.devices.ReactiveLightDimmer', exportable: true) {
    pathToTransmitRF = '/home/pi/TransmitRF.py'
    requirePir = true
    lightSensorPubSubNodes = [ 'devices/lightSensor/state' ];
    pirSensorPubSubNodes = [ 'devices/tvSensor/state', 'devices/pirSensor/state' ]
    turnOnIfLowerThan = 30
    turnOffIfHigherThan = 40
}

lightSensor (class: 'tigase.rpi.home.sensors.light.BH1750', exportable: true) {
    address = 35
}

pirSensor (class: 'tigase.rpi.home.sensors.pir.HC_SR501', exportable: true) {

}

tvSensor (class: 'tigase.rpi.home.sensors.pir.AndroidTv', exportable: true) {
    address = 'http://172.16.0.224/sony/system'
}

w1Master (class: 'tigase.rpi.home.sensors.w1.W1Master', exportable: true) {
    period = 15000
}

#w1-28-0000084a4859 {
#    period = 15000
#}

#tempPublisher (class: 'tigase.rpi.home.app.TemperaturePubSubPublisher', exportable: true) {

devicePubSubPublisher (class: 'tigase.rpi.home.app.pubsub.DevicePubSubPublisher', exportable: true) {
}

pubSubNodesManager (class: 'tigase.rpi.home.app.pubsub.ExtendedPubSubNodesManager', exportable: true) {
    PEP = true
}

lightSensorListener (class: 'tigase.rpi.home.app.LightSensorListener', exportable: true) {
    observes = [ "devices/lightSensor/state", "devices/w1-28-0000084a4859/state" ]
}

deviceConfigManager (class: 'tigase.rpi.home.app.DeviceConfigurationPubSubManager', exportable: true) {

}

# test12 (class: 'tigase.rpi.home.app.Test', exportable: true) {
#
#}
