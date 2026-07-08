package com.investsentinel.api.service;

import com.investsentinel.api.strategy.NotificationStrategy;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationContextService {

    private final Map<String, NotificationStrategy> strategies;

    public NotificationContextService(List<NotificationStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(NotificationStrategy::getChannelName, strategy -> strategy));
    }

    public void routeNotification(String channel, String asset, double price, String message) {
        NotificationStrategy strategy = strategies.get(channel.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Canal de notificação inválido: " + channel);
        }
        strategy.sendAlert(asset, price, message);
    }
}
