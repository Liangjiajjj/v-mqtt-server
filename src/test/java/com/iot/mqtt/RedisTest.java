package com.iot.mqtt;

import com.iot.mqtt.message.handler.message.impl.SubscribeMessageHandler;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import com.iot.mqtt.subscribe.topic.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MqttServerApplication.class)
@FixMethodOrder(MethodSorters.JVM)
public class RedisTest {

    @Autowired
    private SubscribeMessageHandler subscribeMessageHandler;
    @Autowired
    private ISubscribeManager subscribeManager;

    @Test
    public void test() {
        subscribeManager.add(new Subscribe());
        // subscribeMessageHandler.addSubscriptions("", new ArrayList<>());
    }
}
