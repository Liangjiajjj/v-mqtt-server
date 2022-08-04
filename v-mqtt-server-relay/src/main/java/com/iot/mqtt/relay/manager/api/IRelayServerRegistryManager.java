package com.iot.mqtt.relay.manager.api;

import java.net.InetSocketAddress;

public interface IRelayServerRegistryManager {

    InetSocketAddress getInetSocketAddress(String brokerId);

}
