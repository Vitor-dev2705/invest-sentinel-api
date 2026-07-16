package com.investsentinel.api.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.investsentinel.api.service.TelegramVoiceBotService;

@RestController
@RequestMapping("/telegram")
public class TelegramWebhookController {

    private final TelegramVoiceBotService telegramVoiceBotService;

    public TelegramWebhookController(TelegramVoiceBotService telegramVoiceBotService) {
        this.telegramVoiceBotService = telegramVoiceBotService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody Map<String, Object> update) {
        // Log de barreira absoluto para verificar conectividade externa
        System.out.println("[Telegram Controller] >>> REQUISIÇÃO DETECTADA NO ENDPOINT POST /telegram/webhook");
        System.out.println("[Telegram Controller] Payload Bruto Recebido: " + update);

        try {
            telegramVoiceBotService.handleUpdate(update);
        } catch (Exception e) {
            System.err.println("[Telegram Controller] Erro ao processar update: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/set-webhook")
    public ResponseEntity<String> setWebhook(@RequestParam("url") String url,
                                             @RequestParam(value = "token", required = false) String token) {
        System.out.println("[Telegram Controller] Executando comando set-webhook para a URL: " + url);
        boolean ok = telegramVoiceBotService.registerWebhook(url, token);
        return ok ? ResponseEntity.ok("Webhook registrado com sucesso") 
                  : ResponseEntity.badRequest().body("Falha ao registrar webhook");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        System.out.println("[Telegram Controller] Endpoint /telegram/health foi chamado.");
        return ResponseEntity.ok("ok");
    }
}