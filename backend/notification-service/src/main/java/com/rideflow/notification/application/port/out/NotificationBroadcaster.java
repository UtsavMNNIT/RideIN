package com.rideflow.notification.application.port.out;

import com.rideflow.notification.domain.model.Notification;

/** Output port for broadcasting a notification to the cluster (e.g. via Redis Pub/Sub). */
public interface NotificationBroadcaster {

    void broadcast(Notification notification);
}
