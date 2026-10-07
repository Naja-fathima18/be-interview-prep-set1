package com.example.beinterviewprep.product.service;

import com.example.beinterviewprep.product.domain.Category;
import java.math.BigDecimal;

public record ProductCommand(
    String name, Category category, BigDecimal price, int stock, BigDecimal rating) {}
