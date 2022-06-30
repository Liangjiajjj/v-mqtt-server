package com.iot.mqtt.subscribe.repository;

import com.iot.mqtt.subscribe.info.Subscription;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class MemorySubscriptionsRepository implements ISubscriptionsRepository {

    private final Set<Subscription> subscriptions = new ConcurrentSkipListSet<>();

    @Override
    public Set<Subscription> listAllSubscriptions() {
        return Collections.unmodifiableSet(subscriptions);
    }

    @Override
    public void addNewSubscription(Subscription subscription) {
        subscriptions.add(subscription);
    }

    @Override
    public void removeSubscription(String topic, String clientID) {
        subscriptions.stream()
                .filter(s -> s.getTopicFilter().toString().equals(topic) && s.getClientId().equals(clientID))
                .findFirst()
                .ifPresent(subscriptions::remove);
    }
}
