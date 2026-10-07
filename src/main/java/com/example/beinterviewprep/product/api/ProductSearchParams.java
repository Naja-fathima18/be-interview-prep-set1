package com.example.beinterviewprep.product.api;

import com.example.beinterviewprep.product.domain.Category;
import com.example.beinterviewprep.product.service.ProductFilter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductSearchParams(
    Category category,
    @DecimalMin(value = "0", message = "Minimum price cannot be negative") BigDecimal minPrice,
    @DecimalMin(value = "0", message = "Maximum price cannot be negative") BigDecimal maxPrice,
    Boolean inStock,
    @Size(max = 200, message = "Search text must be at most 200 characters") String q) {

  @AssertTrue(message = "minPrice must not be greater than maxPrice")
  public boolean isPriceRangeValid() {
    return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
  }

  public ProductFilter toFilter() {
    return new ProductFilter(category, minPrice, maxPrice, Boolean.TRUE.equals(inStock), q);
  }
}
