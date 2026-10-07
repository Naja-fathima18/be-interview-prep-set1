package com.example.beinterviewprep.product.service;

import com.example.beinterviewprep.product.domain.Category;
import com.example.beinterviewprep.product.domain.Product;
import java.math.BigDecimal;
import java.time.Instant;

public record ProductView(
    Long id,
    String name,
    Category category,
    BigDecimal price,
    int stock,
    BigDecimal rating,
    Instant createdAt) {

  public static ProductView from(Product product) {
    return new ProductView(
        product.getId(),
        product.getName(),
        product.getCategory(),
        product.getPrice(),
        product.getStock(),
        product.getRating(),
        product.getCreatedAt());
  }
}
