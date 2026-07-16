package com.investsentinel.api;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.investsentinel.api.facade.MarketAlertFacade;
import com.investsentinel.api.service.AlertAuditService;
import com.investsentinel.api.service.NotificationContextService;
import com.investsentinel.api.strategy.EmailNotificationStrategy;
import com.investsentinel.api.strategy.TelegramNotificationStrategy;

class MarketAlertFacadeTest {

    private MarketAlertFacade facade;
    private AlertAuditService auditService;

    @BeforeEach
    void setUp() {
        NotificationContextService notificationService = new NotificationContextService(
                List.of(new EmailNotificationStrategy(), new TelegramNotificationStrategy())
        );
        
        this.auditService = new AlertAuditService();
        this.facade = new MarketAlertFacade(notificationService, auditService);
    }

    @Test
    void shouldRecordAlertHistoryWhenTriggeringNotification() {
        String result = facade.triggerAlert("PETR4", 25.5, "email");

        assertThat(result).contains("PETR4");
        assertThat(auditService.getHistory()).hasSize(1);
        assertThat(auditService.getHistory().get(0).asset()).isEqualTo("PETR4");
    }

    @Test
    void shouldRejectInvalidPrices() {
        assertThatThrownBy(() -> facade.triggerAlert("VALE3", 0.0, "telegram"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maior que zero");
    }

    @Test
    void shouldRejectBlankAssetsBeforeTriggeringAlert() {
        assertThatThrownBy(() -> facade.triggerAlert("   ", 25.5, "email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ativo");
    }
}