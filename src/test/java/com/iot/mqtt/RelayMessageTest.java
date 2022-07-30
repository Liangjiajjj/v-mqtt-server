package com.iot.mqtt;

import com.iot.mqtt.relay.cluster.RelayConnection;
import com.iot.mqtt.relay.cluster.RelayConnectionPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MqttServerApplication.class)
@FixMethodOrder(MethodSorters.JVM)
public class RelayMessageTest {

    @Autowired
    private RelayConnectionPool relayConnectionPool;

    @Test
    public void test() throws ExecutionException, InterruptedException, TimeoutException {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 1832);
        Map<Integer, RelayConnection> relayConnectionMap = new HashMap<>();
        for (int i = 0; i < 130; i++) {
            RelayConnection relayConnection = relayConnectionPool.getConnection(i, address).get(10, TimeUnit.SECONDS);
            log.info("client id : {} channel : {}", i, relayConnection.channel());
            relayConnectionMap.put(i, relayConnection);
        }
        for (int i = 0; i < 130; i++) {
            RelayConnection relayConnection = relayConnectionPool.getConnection(i, address).get(10, TimeUnit.SECONDS);
            if (relayConnectionMap.get(i).equals(relayConnection)) {
                log.info("same connection client id : {} channel : {}", i, relayConnection.channel());
            } else {
                throw new RuntimeException("not same connection !!! ");
            }
        }
    }

}
