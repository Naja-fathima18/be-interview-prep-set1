package com.example.beinterviewprep.product.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("catalog.seed")
public record ProductSeedProperties(
    @DefaultValue("true") boolean enabled, @DefaultValue("100") int count) {}
