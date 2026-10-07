package com.example.beinterviewprep.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class CacheConfig {

  @Bean
  public CacheManager cacheManager(CachingProperties properties) {
    CaffeineCacheManager caffeine = new CaffeineCacheManager();
    caffeine.setCaffeine(
        Caffeine.newBuilder()
            .maximumSize(properties.maximumSize())
            .expireAfterWrite(properties.expireAfterWrite())
            .recordStats());
    caffeine.setCacheNames(properties.names());
    caffeine.setAllowNullValues(false);
    return new TransactionAwareCacheManagerProxy(caffeine);
  }
}
