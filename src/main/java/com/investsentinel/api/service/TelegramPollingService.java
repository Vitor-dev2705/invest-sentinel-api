package com.investsentinel.api.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnProperty(name = "telegram.polling.enabled", havingValue = "true", matchIfMissing = true)
public class TelegramPollingService {

    private final TelegramVoiceBotService telegramVoiceBotService;
    private final RestTemplate restTemplate;
    private final String botToken;
    private final String telegramApiBaseUrl;
    private final boolean pollingEnabled;

    private volatile boolean running = false;
    private long lastUpdateId = 0;

    public TelegramPollingService(TelegramVoiceBotService telegramVoiceBotService,
                                   RestTemplate restTemplate,
                                   @Value("${telegram.bot.token:}") String botToken,
                                   @Value("${telegram.api.base-url:https://api.telegram.org}") String telegramApiBaseUrl,
                                   @Value("${telegram.polling.enabled:true}") boolean pollingEnabled) {
        this.telegramVoiceBotService = telegramVoiceBotService;
        this.restTemplate = restTemplate;
        this.botToken = botToken;
        this.telegramApiBaseUrl = telegramApiBaseUrl;
        this.pollingEnabled = pollingEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startPolling() {
        if (!pollingEnabled || botToken == null || botToken.isBlank()) {
            System.out.println("[Telegram Polling] Polling desabilitado nas propriedades. O fluxo dependerá do WebhookController.");
            return;
        }

        System.out.println("[Telegram Polling] Modo Polling Ativo. Limpando possíveis webhooks antigos...");
        removeWebhook();

        running = true;
        Thread pollingThread = new Thread(this::pollLoop, "telegram-polling");
        pollingThread.setDaemon(true);
        pollingThread.start();
        System.out.println("[Telegram Polling] Polling iniciado com sucesso! Escutando eventos do bot...");
    }

    public void stop() {
        this.running = false;
    }

    private void removeWebhook() {
        try {
            String url = telegramApiBaseUrl + "/bot" + botToken + "/deleteWebhook?drop_pending_updates=true";
            restTemplate.getForObject(url, Map.class);
            System.out.println("[Telegram Polling] Webhook removido e atualizações pendentes limpas com sucesso.");
        } catch (Exception e) {
            System.err.println("[Telegram Polling] Erro ou aviso ao remover webhook: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void pollLoop() {
        while (running) {
            try {
                String url = telegramApiBaseUrl + "/bot" + botToken
                        + "/getUpdates?timeout=30&offset=" + (lastUpdateId + 1);

                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
                    Thread.sleep(5000); 
                    continue;
                }

                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("result");
                if (results == null || results.isEmpty()) {
                    continue;
                }

                for (Map<String, Object> update : results) {
                    Number updateIdNum = (Number) update.get("update_id");
                    if (updateIdNum != null) {
                        lastUpdateId = updateIdNum.longValue();
                    }

                    try {
                        telegramVoiceBotService.handleUpdate(update);
                    } catch (Exception ex) {
                        System.err.println("[Telegram Polling] Erro isolado ao processar o update id [" + lastUpdateId + "]: " + ex.getMessage());
                    }
                }
            } catch (InterruptedException ie) {
                System.out.println("[Telegram Polling] Thread de polling interrompida.");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[Telegram Polling] Conexão perdida ou erro na requisição getUpdates: " + e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        System.out.println("[Telegram Polling] Polling encerrado.");
    }
}