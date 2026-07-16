package com.investsentinel.api.tool;

import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import com.investsentinel.api.controller.model.AlertRequest;
import com.investsentinel.api.controller.model.AssetPriceRequest;
import com.investsentinel.api.facade.MarketAlertFacade;
import com.investsentinel.api.service.StockPriceService;

@Configuration
public class FinancialTools {

    private final MarketAlertFacade alertFacade;
    private final StockPriceService stockPriceService;

    public FinancialTools(MarketAlertFacade alertFacade, StockPriceService stockPriceService) {
        this.alertFacade = alertFacade;
        this.stockPriceService = stockPriceService;
    }

    @Bean
    @Description("Cadastra ou dispara um alerta de preço para um ativo específico escolhendo o canal de notificação (TELEGRAM ou EMAIL).")
    public Function<AlertRequest, String> triggerMarketAlert() {
        return request -> {
            try {
                String priceRaw = request.price();
                if (priceRaw == null || priceRaw.isBlank()) {
                    return "Falha ao processar o alerta: O valor do preço não foi fornecido.";
                }
                String priceCleaned = priceRaw.replace("R$", "").replace("$", "").trim();
                Double numericPrice = Double.valueOf(priceCleaned);

                return alertFacade.triggerAlert(request.asset(), numericPrice, request.channel());
            } catch (Exception e) {
                return "Falha ao processar o alerta: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Busca o preço ou cotação atual de mercado para um ativo específico da bolsa de valores (ex: VALE3, PETR4, AAPL).")
    public Function<AssetPriceRequest, String> getCurrentAssetPrice() {
        return request -> {
            if (request.asset() == null || request.asset().isBlank()) {
                return "Erro: O nome do ativo não foi informado.";
            }
            return stockPriceService.getCurrentPrice(request.asset());
        };
    }
}