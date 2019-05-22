# Tigase IoT Framework

Tigase IoT Framework was created as an easy to use framework for creation of devices with support for IoT. It started as with support for Raspberry Pi devices but most of it is hardware agnostic and can be used on any platform capable of running Java.


Project consists of following modules:


* **runtime** 

Runtime is a wrapper over  [Jaxmpp Bot Framework](https://projects.tigase.org/projects/jaxmpp-bot-framework)  which provides easy to use base application/framework to create IoT devices which communicate using XMPP and PubSub. 

* **devices** 

Module contains implementation of devices/sensors and base classes which should be used to create new drivers for sensors

* **devices-rpi** 

Here you will find drivers for a few sensors which can be used on Raspberry Pi.

_Can be replaced by alternative implementations for other hardware_

* **client-library** 

It is a library created for use in a XMPP clients based on Jaxmpp which will act as a user interface for controlling IoT devices using this framework.

* **client** 

It is an example client which is web UI for controlling IoT devices. It's created in GWT and uses *client-library*.

* **documentation** 

Contains up-to-date version of documentation in asciidoc.

For generated version of the documentation download [Tigase IoT Framework documentation](https://tigase.tech/attachments/6324/tigase-iot-framework_20180616.zip)
