package com.investsentinel.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.investsentinel.api.service.VoiceAssistantService;

@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    private final VoiceAssistantService voiceAssistantService;

    public VoiceController(VoiceAssistantService voiceAssistantService) {
        this.voiceAssistantService = voiceAssistantService;
    }

    @PostMapping("/process")
    public ResponseEntity<String> processVoice(@RequestParam("file") MultipartFile file) {
        try {
            String responseText = voiceAssistantService.processVoiceCommandToText(file);
            return ResponseEntity.ok(responseText);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro interno ao processar o comando de voz: " + e.getMessage());
        }
    }
}