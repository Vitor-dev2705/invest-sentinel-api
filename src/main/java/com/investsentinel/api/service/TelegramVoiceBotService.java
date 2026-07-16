package com.investsentinel.api.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class TelegramVoiceBotService {

    private final VoiceAssistantService voiceAssistantService;
    private final RestTemplate restTemplate;
    private final String botToken;
    private final String telegramApiBaseUrl;

    public TelegramVoiceBotService(VoiceAssistantService voiceAssistantService,
                                   RestTemplate restTemplate,
                                   @Value("${telegram.bot.token:}") String botToken,
                                   @Value("${telegram.api.base-url:https://api.telegram.org}") String telegramApiBaseUrl) {
        this.voiceAssistantService = voiceAssistantService;
        this.restTemplate = restTemplate;
        this.botToken = botToken;
        this.telegramApiBaseUrl = telegramApiBaseUrl.endsWith("/")
                ? telegramApiBaseUrl.substring(0, telegramApiBaseUrl.length() - 1)
                : telegramApiBaseUrl;
    }

    public boolean registerWebhook(String webhookUrl) {
        return registerWebhook(webhookUrl, botToken);
    }

    @SuppressWarnings("unchecked")
    public boolean registerWebhook(String webhookUrl, String token) {
        String effectiveToken = (token == null || token.isBlank()) ? botToken : token;
        if (effectiveToken == null || effectiveToken.isBlank() || webhookUrl == null || webhookUrl.isBlank()) {
            return false;
        }

        String url = telegramApiBaseUrl + "/bot" + effectiveToken + "/setWebhook?url=" + webhookUrl;
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return response != null && Boolean.TRUE.equals(response.get("ok"));
    }

    @SuppressWarnings("unchecked")
    public boolean handleUpdate(Map<String, Object> update) {
        if (botToken == null || botToken.isBlank()) {
            System.err.println("[Telegram Bot] Erro: Token do bot não configurado nas propriedades.");
            return false;
        }

        System.out.println("[Telegram Bot] Payload recebido no webhook: " + update);

        Map<String, Object> message = (Map<String, Object>) update.get("message");
        if (message == null) {
            return false;
        }

        Map<String, Object> chat = (Map<String, Object>) message.get("chat");
        if (chat == null || chat.get("id") == null) {
            return false;
        }

        Long chatId = ((Number) chat.get("id")).longValue();

        // 1. Tenta capturar dados do campo 'voice' ou 'audio'
        Map<String, Object> voiceData = null;
        if (message.get("voice") != null) {
            voiceData = (Map<String, Object>) message.get("voice");
        } else if (message.get("audio") != null) {
            voiceData = (Map<String, Object>) message.get("audio");
        }

        // 2. Se for uma mensagem de texto comum, processa pelo fluxo de texto da IA
        if (voiceData == null) {
            String text = (String) message.get("text");
            if (text == null || text.isBlank()) {
                return false; 
            }

            System.out.println("[Telegram Bot] Processando comando de texto do Chat ID [" + chatId + "]: " + text);
            try {
                String responseText = voiceAssistantService.processTextCommand(text);
                sendMessage(chatId, responseText);
                return true;
            } catch (Exception ex) {
                System.err.println("[Telegram Bot] Erro ao processar comando de texto: " + ex.getMessage());
                sendMessage(chatId, "Ocorreu um erro ao processar sua solicitação.");
                return false;
            }
        }

        // 3. Fluxo de Áudio
        String fileId = (String) voiceData.get("file_id");
        if (fileId == null || fileId.isBlank()) {
            return false;
        }

        System.out.println("[Telegram Bot] Processando mensagem de áudio do Chat ID: " + chatId);

        try {
            // Faz o download do áudio (.ogg)
            byte[] audioBytes = downloadFile(fileId);
            System.out.println("[Telegram Bot] Download concluído. Tamanho: " + audioBytes.length + " bytes.");

            System.out.println("[Telegram Bot] Enviando áudio para transcrição e inteligência...");
            String responseText = voiceAssistantService.processVoiceCommandToText(audioBytes, "telegram-audio.ogg");

            if (responseText == null || responseText.isBlank()) {
                System.err.println("[Telegram Bot] A IA retornou um texto vazio.");
                sendMessage(chatId, "Desculpe, não consegui compreender o seu áudio.");
                return false;
            }

            System.out.println("[Telegram Bot] Resposta gerada com sucesso pela IA: " + responseText);

            sendMessage(chatId, responseText);
            return true;

        } catch (Exception ex) {
            System.err.println("[Telegram Bot] Erro fatal no handleUpdate: " + ex.getMessage());
            ex.printStackTrace();
            sendMessage(chatId, "Ocorreu um erro interno ao processar sua mensagem de voz.");
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private byte[] downloadFile(String fileId) {
        String getFileUrl = telegramApiBaseUrl + "/bot" + botToken + "/getFile?file_id=" + fileId;
        Map<String, Object> response = restTemplate.getForObject(getFileUrl, Map.class);
        if (response == null || !response.containsKey("result")) {
            throw new IllegalStateException("Não foi possível localizar os metadados do arquivo no Telegram.");
        }
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        if (result == null) {
            throw new IllegalStateException("O campo 'result' da API getFile retornou nulo.");
        }
        String filePath = (String) result.get("file_path");
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalStateException("O caminho do arquivo físico (file_path) não foi retornado pelo Telegram.");
        }
        
        String downloadUrl = telegramApiBaseUrl + "/file/bot" + botToken + "/" + filePath;
        return restTemplate.getForObject(downloadUrl, byte[].class);
    }

    private void sendMessage(Long chatId, String text) {
        String url = telegramApiBaseUrl + "/bot" + botToken + "/sendMessage";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId.toString());
        body.add("text", text);
        body.add("parse_mode", "HTML");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            System.out.println("[Telegram Bot] Enviando resposta em texto...");
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("[Telegram Bot] Mensagem enviada com sucesso! Resposta: " + response.getBody());
        } catch (Exception e) {
            System.err.println("[Telegram Bot] Falha ao enviar mensagem de texto. Tentando enviar sem formatação HTML...");
            try {
                body.remove("parse_mode");
                HttpEntity<MultiValueMap<String, String>> plainRequest = new HttpEntity<>(body, headers);
                restTemplate.postForEntity(url, plainRequest, String.class);
            } catch (Exception ex) {
                System.err.println("[Telegram Bot] Falha crítica ao enviar mensagem de texto pura: " + ex.getMessage());
            }
        }
    }
}