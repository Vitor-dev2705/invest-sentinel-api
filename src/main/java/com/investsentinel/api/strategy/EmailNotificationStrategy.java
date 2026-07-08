package com.investsentinel.api.strategy;

import org.springframework.stereotype.Component;

@Component
public class EmailNotificationStrategy implements NotificationStrategy {
    @Override
    public void sendAlert(String asset, double price, String message) {
        System.out.println(String.format("[Email Service] Enviando e-mail de alerta: %s está cotado a R$ %.2f.", asset, price));
    }

    @Override
    public String getChannelName() {
        return "EMAIL";
    }
}