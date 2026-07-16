package com.investsentinel.api.controller.model;


public record AlertRequest(
    String asset,
    String price,
    String channel
) {}