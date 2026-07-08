package com.investsentinel.api.controller;

import com.investsentinel.api.facade.MarketAlertFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final MarketAlertFacade alertFacade;

    public AlertController(MarketAlertFacade alertFacade) {
        this.alertFacade = alertFacade;
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerAssetAlert(
            @RequestParam String asset,
            @RequestParam double price,
            @RequestParam String channel) {
         
        alertFacade.executeAlertPipeline(asset, price, channel);
        return ResponseEntity.ok("Pipeline de monitoramento disparado com sucesso via: " + channel);
    }
}
