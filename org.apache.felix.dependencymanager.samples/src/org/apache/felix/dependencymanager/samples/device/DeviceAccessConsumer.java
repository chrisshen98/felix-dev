package org.apache.felix.dependencymanager.samples.device;

import java.util.Map;

import org.apache.felix.dependencymanager.samples.util.Helper;

public class DeviceAccessConsumer {
    void add(Map<String, Object> props, DeviceAccess deviceAccess) {
        Helper.log("device", "DeviceAccessConsumer: Handling device access: id=" + props.get("device.id") 
            + "\n\t device=" + deviceAccess.getDevice() 
            + "\n\t device parameter=" + deviceAccess.getDeviceParameter()
            + "\n\t device access properties=" + props);
    }
}
