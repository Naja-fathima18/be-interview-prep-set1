package com.example.beinterviewprep.product.service;

import com.example.beinterviewprep.product.domain.Category;
import java.math.BigDecimal;

public record ProductFilter(
    Category category,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    boolean inStockOnly,
    String nameQuery) {}
