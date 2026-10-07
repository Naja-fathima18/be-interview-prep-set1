package com.example.beinterviewprep.common.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.cache")
public record CachingProperties(
    @DefaultValue("products") List<String> names,
    @DefaultValue("10000") long maximumSize,
    @DefaultValue("PT10M") Duration expireAfterWrite) {}
