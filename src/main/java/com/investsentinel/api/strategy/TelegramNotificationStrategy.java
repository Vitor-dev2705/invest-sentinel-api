package com.investsentinel.api.strategy;

import org.springframework.stereotype.Component;

@Component
public class TelegramNotificationStrategy implements NotificationStrategy {
    @Override
    public void sendAlert(String asset, double price, String message) {
        // Correção das porcentagens para o String.format funcionar perfeitamente
        System.out.println(String.format("[Telegram Bot] Alerta Ativado! %s atingiu R$ %.2f. %s", asset, price, message));
    }

    @Override
    public String getChannelName() {
        return "TELEGRAM";
    }
}