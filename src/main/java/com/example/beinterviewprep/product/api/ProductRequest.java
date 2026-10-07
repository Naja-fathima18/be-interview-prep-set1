package com.example.beinterviewprep.product.api;

import com.example.beinterviewprep.product.domain.Category;
import com.example.beinterviewprep.product.service.ProductCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must be at most 200 characters")
        String name,
    @NotNull(message = "Category is required") Category category,
    @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Price must have at most 2 decimal places")
        BigDecimal price,
    @NotNull(message = "Stock is required") @PositiveOrZero(message = "Stock cannot be negative")
        Integer stock,
    @NotNull(message = "Rating is required")
        @DecimalMin(value = "0.0", message = "Rating must be between 0 and 5")
        @DecimalMax(value = "5.0", message = "Rating must be between 0 and 5")
        @Digits(integer = 1, fraction = 1, message = "Rating must have at most 1 decimal place")
        BigDecimal rating) {

  public ProductCommand toCommand() {
    return new ProductCommand(name.strip(), category, price, stock, rating);
  }
}
