package com.investsentinel.api.facade;

import com.investsentinel.api.service.NotificationContextService;
import org.springframework.stereotype.Component;

@Component
public class MarketAlertFacade {

    private final NotificationContextService notificationService;

    public MarketAlertFacade(NotificationContextService notificationService) {
        this.notificationService = notificationService;
    }

    public void executeAlertPipeline(String asset, double currentPrice, String targetChannel) {
        String businessMessage = "Análise do Sentinel: Volume de negociação acima da média.";
         
        if (currentPrice <= 0) {
            throw new IllegalStateException("O preço do ativo precisa ser maior que zero.");
        }

        notificationService.routeNotification(targetChannel, asset, currentPrice, businessMessage);
    }
}
