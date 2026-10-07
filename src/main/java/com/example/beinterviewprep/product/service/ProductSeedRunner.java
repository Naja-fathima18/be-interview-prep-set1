package com.example.beinterviewprep.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(ProductSeedProperties.class)
@ConditionalOnProperty(name = "catalog.seed.enabled", matchIfMissing = true)
public class ProductSeedRunner implements ApplicationRunner {

  private final ProductSeeder productSeeder;
  private final ProductSeedProperties properties;

  @Override
  public void run(ApplicationArguments args) {
    productSeeder.seedIfEmpty(properties.count());
  }
}
