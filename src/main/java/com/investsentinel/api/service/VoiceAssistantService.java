package com.investsentinel.api.service;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VoiceAssistantService {

    private final ObjectProvider<OpenAiAudioTranscriptionModel> transcriptionModelProvider;
    private final ObjectProvider<OpenAiChatModel> chatModelProvider;
    private final RestTemplate restTemplate;
    private final String fallbackMessage;
    private final boolean useRealAi; 
    private final String googleApiKey;
    private final boolean useGoogleAi;

    @Autowired
    public VoiceAssistantService(ObjectProvider<OpenAiAudioTranscriptionModel> transcriptionModelProvider,
            ObjectProvider<OpenAiChatModel> chatModelProvider,
            @Value("${spring.ai.openai.enabled:false}") boolean openAiEnabled,
            @Value("${spring.ai.openai.api-key:}") String openAiApiKey,
            @Value("${gemini.api.key:${google.api.key:}}") String googleApiKey) {
        this(transcriptionModelProvider,
                chatModelProvider,
                "Não foi possível conectar à IA no momento. O fluxo de voz entrou em modo de teste local.",
                openAiEnabled && !openAiApiKey.isBlank() && !"demo-key".equalsIgnoreCase(openAiApiKey.trim()),
                googleApiKey,
                new RestTemplate());
    }

    public VoiceAssistantService(ObjectProvider<OpenAiAudioTranscriptionModel> transcriptionModelProvider,
            ObjectProvider<OpenAiChatModel> chatModelProvider,
            String fallbackMessage,
            boolean useRealAi) {
        this(transcriptionModelProvider,
                chatModelProvider,
                fallbackMessage,
                useRealAi,
                "",
                new RestTemplate());
    }

    public VoiceAssistantService(OpenAiAudioTranscriptionModel transcriptionModel,
            OpenAiChatModel chatModel,
            String fallbackMessage) {
        this(null,
                null,
                fallbackMessage,
                false,
                "",
                new RestTemplate());
    }

    public VoiceAssistantService(OpenAiAudioTranscriptionModel transcriptionModel,
            OpenAiChatModel chatModel,
            String fallbackMessage,
            boolean useRealAi) {
        this(null,
                null,
                fallbackMessage,
                useRealAi,
                "",
                new RestTemplate());
    }

    private VoiceAssistantService(ObjectProvider<OpenAiAudioTranscriptionModel> transcriptionModelProvider,
            ObjectProvider<OpenAiChatModel> chatModelProvider,
            String fallbackMessage,
            boolean useRealAi,
            String googleApiKey,
            RestTemplate restTemplate) {
        this.transcriptionModelProvider = transcriptionModelProvider;
        this.chatModelProvider = chatModelProvider;
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
        this.fallbackMessage = fallbackMessage;
        this.useRealAi = useRealAi;
        this.googleApiKey = googleApiKey == null ? "" : googleApiKey.trim();
        this.useGoogleAi = !this.googleApiKey.isBlank();
    }


    public String processTextCommand(String text) {
        if (text == null || text.isBlank()) {
            return "O comando de texto enviado está vazio.";
        }

        if (!useRealAi && !useGoogleAi) {
            System.out.println("[Invest Sentinel IA] Configuração local ativa. Retornando resposta fallback para texto.");
            return fallbackMessage;
        }

        if (useGoogleAi) {
            try {
                return generateTextWithGoogle(text);
            } catch (Exception ex) {
                System.err.println("[Invest Sentinel IA] Falha ao processar texto com Gemini. Retornando fallback: " + ex.getMessage());
                return fallbackMessage;
            }
        }

        try {
            OpenAiChatModel chatModel = chatModelProvider.getIfAvailable();
            if (chatModel == null) {
                System.out.println("[Invest Sentinel IA] Bean do chatModel (Groq) indisponível para processar texto. Retornando fallback.");
                return fallbackMessage;
            }

            ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultSystem("Você é o assistente de voz oficial do Invest Sentinel. "
                            + "Se o usuário pedir para monitorar, criar alerta ou cadastrar uma cotação, "
                            + "use a função 'triggerMarketAlert' extraindo o ativo, preço e canal. "
                            + "Se o usuário apenas perguntar o preço, valor ou cotação atual de uma ação/ativo de forma direta "
                            + "(ex: 'Qual o valor da Vale?', 'Cotação atual da PETR4'), use a função 'getCurrentAssetPrice' extraindo o ativo desejado. "
                            + "Seja breve, direto e profissional nas respostas.")
                    .defaultFunctions("triggerMarketAlert", "getCurrentAssetPrice") // Habilitando ambas as ferramentas
                    .build();

            String aiResponseText = chatClient.prompt()
                    .user(text)
                    .call()
                    .content();

            System.out.println("[Invest Sentinel IA] Resposta de texto da Groq: " + aiResponseText);
            return aiResponseText;

        } catch (Exception ex) {
            System.err.println("[Invest Sentinel IA] Falha ao usar Groq para texto. Usando fallback. " + ex.getMessage());
            return fallbackMessage;
        }
    }

    /**
     * Ponto de entrada que aceita MultipartFile e retorna a resposta em TEXTO.
     */
    public String processVoiceCommandToText(MultipartFile audioFile) throws Exception {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("O arquivo de áudio não pode estar vazio.");
        }
        return processVoiceCommandToText(audioFile.getBytes(), audioFile.getOriginalFilename());
    }

    /**
     * Processa os bytes de áudio recebidos e retorna a resposta final em TEXTO.
     */
    public String processVoiceCommandToText(byte[] audioBytes, String originalFilename) throws Exception {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new IllegalArgumentException("O arquivo de áudio não pode estar vazio.");
        }

        if (!useRealAi && !useGoogleAi) {
            System.out.println("[Invest Sentinel IA] Configuração local ativa. Retornando resposta fallback em texto.");
            return fallbackMessage;
        }

        // Fluxo Google (STT + Gemini)
        if (useGoogleAi) {
            try {
                String userTextCommand = transcribeWithGoogle(audioBytes, originalFilename);
                if (userTextCommand == null || userTextCommand.isBlank()) {
                    return "Não consegui entender o áudio enviado.";
                }
                return generateTextWithGoogle(userTextCommand);
            } catch (Exception ex) {
                System.err.println("[Invest Sentinel IA] Falha ao usar a API do Google. Retornando fallback. " + ex.getMessage());
                return fallbackMessage;
            }
        }

        // Fluxo Spring AI (Groq Whisper + Groq Chat Model)
        try {
            OpenAiAudioTranscriptionModel transcriptionModel = transcriptionModelProvider.getIfAvailable();
            OpenAiChatModel chatModel = chatModelProvider.getIfAvailable();

            if (transcriptionModel == null || chatModel == null) {
                System.out.println("[Invest Sentinel IA] Beans de IA (Groq) não disponíveis. Retornando fallback.");
                return fallbackMessage;
            }

            ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultSystem("Você é o assistente de voz oficial do Invest Sentinel. "
                            + "Se o usuário pedir para monitorar, criar alerta ou cadastrar uma cotação, "
                            + "use a função 'triggerMarketAlert' extraindo o ativo, preço e canal. "
                            + "Se o usuário apenas perguntar o preço, valor ou cotação atual de uma ação/ativo de forma direta "
                            + "(ex: 'Quanto tá a Vale?', 'Cotação atual da PETR4', 'preço de mglu3'), use a função 'getCurrentAssetPrice' extraindo o ativo desejado. "
                            + "Seja breve, direto e profissional nas respostas.")
                    .defaultFunctions("triggerMarketAlert", "getCurrentAssetPrice") // Habilitando ambas as ferramentas
                    .build();

            Resource audioResource = new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            };

            String userTextCommand = transcriptionModel
                    .call(new AudioTranscriptionPrompt(audioResource))
                    .getResult()
                    .getOutput();

            System.out.println("[Invest Sentinel IA] Texto Transcrito pela Groq Whisper: " + userTextCommand);

            if (userTextCommand == null || userTextCommand.isBlank()) {
                return "Não consegui entender o áudio enviado.";
            }

            String aiResponseText = chatClient.prompt()
                    .user(userTextCommand)
                    .call()
                    .content();

            System.out.println("[Invest Sentinel IA] Resposta de texto da Groq: " + aiResponseText);
            return aiResponseText;

        } catch (Exception ex) {
            System.err.println("[Invest Sentinel IA] Falha ao usar Groq. Usando fallback. " + ex.getMessage());
            return fallbackMessage;
        }
    }

    @SuppressWarnings("unchecked")
    private String transcribeWithGoogle(byte[] audioBytes, String originalFilename) {
        String endpoint = "https://speech.googleapis.com/v1/speech:recognize?key=" + googleApiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("encoding", originalFilename != null && originalFilename.toLowerCase().endsWith(".wav") ? "LINEAR16" : "OGG_OPUS");
        config.put("sampleRateHertz", 16000);
        config.put("languageCode", "pt-BR");
        config.put("model", "latest_long");

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("config", config);
        Map<String, Object> audio = new LinkedHashMap<>();
        audio.put("content", Base64.getEncoder().encodeToString(audioBytes));
        requestBody.put("audio", audio);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        Map<String, Object> response = restTemplate.postForObject(endpoint, entity, Map.class);
        if (response == null || response.get("results") == null) {
            return "";
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        if (results.isEmpty()) {
            return "";
        }

        List<Map<String, Object>> alternatives = (List<Map<String, Object>>) results.get(0).get("alternatives");
        if (alternatives == null || alternatives.isEmpty()) {
            return "";
        }

        return (String) alternatives.get(0).get("transcript");
    }

    @SuppressWarnings("unchecked")
    private String generateTextWithGoogle(String userTextCommand) {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + googleApiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("text", "Você é o assistente de voz oficial do Invest Sentinel. Responda em português, de forma breve, direta e profissional. O usuário disse: " + userTextCommand);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("parts", List.of(textPart));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(endpoint, entity, Map.class);
        if (response == null || response.get("candidates") == null) {
            return "Não foi possível gerar uma resposta agora.";
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates.isEmpty()) {
            return "Não foi possível gerar uma resposta agora.";
        }

        Map<String, Object> candidate = candidates.get(0);
        Map<String, Object> candidateContent = (Map<String, Object>) candidate.get("content");
        if (candidateContent == null || candidateContent.get("parts") == null) {
            return "Não foi possível gerar uma resposta agora.";
        }

        List<Map<String, Object>> parts = (List<Map<String, Object>>) candidateContent.get("parts");
        if (parts.isEmpty()) {
            return "Não foi possível gerar uma resposta agora.";
        }

        return (String) parts.get(0).get("text");
    }
}