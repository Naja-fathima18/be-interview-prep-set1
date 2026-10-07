package com.example.beinterviewprep.product.api;

import com.example.beinterviewprep.product.domain.Category;
import com.example.beinterviewprep.product.service.ProductView;
import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
    Long id,
    String name,
    Category category,
    BigDecimal price,
    int stock,
    BigDecimal rating,
    Instant createdAt) {

  public static ProductResponse from(ProductView product) {
    return new ProductResponse(
        product.id(),
        product.name(),
        product.category(),
        product.price(),
        product.stock(),
        product.rating(),
        product.createdAt());
  }
}
