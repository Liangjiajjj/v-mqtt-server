package com.iot.mqtt;

import com.iot.mqtt.message.handler.message.impl.SubscribeMessageHandler;
import com.iot.mqtt.subscribe.Subscribe;
import com.iot.mqtt.subscribe.manager.ISubscribeManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.redisson.api.DeletedObjectListener;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MqttServerApplication.class)
@FixMethodOrder(MethodSorters.JVM)
public class RedisTest {

    @Resource
    private SubscribeMessageHandler subscribeMessageHandler;
    @Resource
    private ISubscribeManager subscribeManager;
    @Resource
    private RedissonClient redissonClient;

    @Test
    public void test() {
        subscribeManager.add(new Subscribe());
        // subscribeMessageHandler.addSubscriptions("", new ArrayList<>());
    }

    @Test
    public void testDeletedListener() throws IOException, InterruptedException {
        RBucket<Integer> al = redissonClient.getBucket("test");
        al.set(1);
        CountDownLatch latch = new CountDownLatch(1);
        al.addListener(new DeletedObjectListener() {
            @Override
            public void onDeleted(String name) {
                latch.countDown();
            }
        });
        al.delete();
        latch.await(1, TimeUnit.SECONDS);
        Thread.sleep(1111111);
    }

}
