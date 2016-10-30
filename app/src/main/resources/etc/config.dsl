xmppService () {
#    default () {
#        jid = "test@example.com"
#        password = "Pa$$w0rd"
#        ignoreCertificateErrors = true
#    }
}

#presencePublisherDemo (class: 'tigase.rpi.home.runtime.PresencePublisherDemo', exportable: true) {
#
#}

lightsDimmer (class: 'tigase.rpi.home.devices.LightDimmer', exportable: true) {
    pathToTransmitRF = '/home/pi/TransmitRF.py'
}

lightSensor (class: 'tigase.rpi.home.sensors.light.BH1750', exportable: true) {
    address = 35
}

w1Master (class: 'tigase.rpi.home.sensors.w1.W1Master', exportable: true) {
    period = 15000
}

tempPublisher (class: 'tigase.rpi.home.app.TemperaturePubSubPublisher', exportable: true) {
    PEP = true
}

# test12 (class: 'tigase.rpi.home.app.Test', exportable: true) {
#
#}
