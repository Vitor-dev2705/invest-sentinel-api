package com.investsentinel.api.facade;

import org.springframework.stereotype.Component;

import com.investsentinel.api.service.AlertAuditService;
import com.investsentinel.api.service.NotificationContextService;

@Component
public class MarketAlertFacade {

    private final NotificationContextService notificationService;
    private final AlertAuditService auditService;

    public MarketAlertFacade(NotificationContextService notificationService, AlertAuditService auditService) {
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    public String triggerAlert(String asset, double currentPrice, String targetChannel) {
        String businessMessage = "Análise do Sentinel: Volume de negociação acima da média.";

        if (asset == null || asset.isBlank()) {
            throw new IllegalArgumentException("O ativo não pode estar vazio.");
        }

        if (currentPrice <= 0) {
            throw new IllegalStateException("O preço do ativo precisa ser maior que zero.");
        }

        notificationService.routeNotification(targetChannel, asset, currentPrice, businessMessage);
        auditService.record(asset, currentPrice, targetChannel);

        return String.format("Alerta disparado para %s no valor de R$ %.2f via %s.", asset, currentPrice, targetChannel);
    }

    public void executeAlertPipeline(String asset, double currentPrice, String targetChannel) {
        triggerAlert(asset, currentPrice, targetChannel);
    }
}
