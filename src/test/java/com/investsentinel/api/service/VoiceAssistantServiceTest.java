package com.investsentinel.api.service;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class VoiceAssistantServiceTest {

    @Mock
    private ObjectProvider<OpenAiAudioTranscriptionModel> transcriptionModelProvider;

    @Mock
    private ObjectProvider<OpenAiChatModel> chatModelProvider;

    @Mock
    private RestTemplate restTemplate;

    private VoiceAssistantService voiceAssistantService;
    private final String fallbackMsg = "Não foi possível conectar à IA no momento. O fluxo de voz entrou em modo de teste local.";

    @Test
    public void testProcessTextCommandFallback() {
        // O setup padrão já cria com useRealAi = false
        String result = voiceAssistantService.processTextCommand("Olá");
        assertEquals(fallbackMsg, result);
    }

    @Test
    public void testProcessTextCommandEmpty() {
        String result = voiceAssistantService.processTextCommand("   ");
        assertEquals("O comando de texto enviado está vazio.", result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProcessTextCommandGoogleGeminiSuccess() {
        voiceAssistantService = new VoiceAssistantService(
                transcriptionModelProvider,
                chatModelProvider,
                fallbackMsg,
                false
        );

        ReflectionTestUtils.setField(voiceAssistantService, "googleApiKey", "fake-google-key");
        ReflectionTestUtils.setField(voiceAssistantService, "useGoogleAi", true);
        ReflectionTestUtils.setField(voiceAssistantService, "restTemplate", restTemplate);

        Map<String, Object> mockResponse = Map.of(
                "candidates", List.of(
                        Map.of("content", Map.of("parts", List.of(Map.of("text", "Olá! Como posso ajudar você no Invest Sentinel hoje?"))))
                )
        );

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        String result = voiceAssistantService.processTextCommand("Oi");
        assertEquals("Olá! Como posso ajudar você no Invest Sentinel hoje?", result);
    }

    @Test
    public void testProcessVoiceCommandEmptyFileThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "audio.ogg", "audio/ogg", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> {
            voiceAssistantService.processVoiceCommandToText(emptyFile);
        });
    }

    @Test
    public void testProcessVoiceCommandNullBytesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            voiceAssistantService.processVoiceCommandToText(null, "audio.ogg");
        });
    }
}