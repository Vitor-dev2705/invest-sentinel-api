package com.investsentinel.api.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class StockPriceService {

    private final RestTemplate restTemplate;

    public StockPriceService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getCurrentPrice(String asset) {
        try {
            String ticker = asset.trim().toUpperCase();
            String url = "https://brapi.dev/api/quote/" + ticker;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                if (!results.isEmpty()) {
                    Map<String, Object> stockData = results.get(0);
                    Double regularMarketPrice = (Double) stockData.get("regularMarketPrice");
                    String currency = (String) stockData.get("currency");
                    String shortName = (String) stockData.get("shortName");

                    return String.format("O preço atual de %s (%s) é %s %.2f.", 
                            ticker, shortName, currency, regularMarketPrice);
                }
            }
            return "Não foi possível encontrar a cotação para o ativo " + ticker;
        } catch (Exception e) {
            return "Erro ao buscar cotação de " + asset + ": " + e.getMessage();
        }
    }
}