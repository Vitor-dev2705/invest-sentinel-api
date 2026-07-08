package com.investsentinel.api.strategy;

public interface NotificationStrategy {
    void sendAlert(String asset, double price, String message);
    String getChannelName();
}
