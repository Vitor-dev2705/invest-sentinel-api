package com.investsentinel.api.service;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.client.RestTemplate;

class TelegramVoiceBotServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldIgnoreUpdatesWhenTelegramBotIsNotConfigured() {
        VoiceAssistantService voiceAssistantService = new VoiceAssistantService(
                (ObjectProvider<OpenAiAudioTranscriptionModel>) null, 
                (ObjectProvider<OpenAiChatModel>) null, 
                "fallback", 
                false
        );

        TelegramVoiceBotService service = new TelegramVoiceBotService(
                voiceAssistantService,
                new RestTemplate(),
                "",
                "https://api.telegram.org"
        );

        Map<String, Object> update = Map.of(
                "message", Map.of(
                        "chat", Map.of("id", 123L),
                        "voice", Map.of("file_id", "abc123")
                )
        );

        boolean handled = service.handleUpdate(update);

        assertThat(handled).isFalse();
    }
}