package com.iot.mqtt.subscribe.repository;

import com.iot.mqtt.subscribe.info.Subscription;

import java.util.Set;

public interface ISubscriptionsRepository {

    Set<Subscription> listAllSubscriptions();

    void addNewSubscription(Subscription subscription);

    void removeSubscription(String topic, String clientID);

}
