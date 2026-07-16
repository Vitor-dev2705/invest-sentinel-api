package com.investsentinel.api.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AlertAuditService {

    private final List<AlertAuditEntry> history = new ArrayList<>();

    public void record(String asset, double price, String channel) {
        history.add(new AlertAuditEntry(
                asset.trim().toUpperCase(),
                price,
                channel.trim().toUpperCase(),
                LocalDateTime.now()
        ));
    }

    public List<AlertAuditEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public record AlertAuditEntry(String asset, double price, String channel, LocalDateTime createdAt) {
    }
}
