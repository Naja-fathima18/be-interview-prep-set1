package com.example.beinterviewprep.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Category category;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(nullable = false)
  private int stock;

  @Column(nullable = false, precision = 2, scale = 1)
  private BigDecimal rating;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public Product(
      String name,
      Category category,
      BigDecimal price,
      int stock,
      BigDecimal rating,
      Instant createdAt) {
    this.name = name;
    this.category = category;
    this.price = price;
    this.stock = stock;
    this.rating = rating;
    this.createdAt = createdAt;
  }

  public void update(
      String name, Category category, BigDecimal price, int stock, BigDecimal rating) {
    this.name = name;
    this.category = category;
    this.price = price;
    this.stock = stock;
    this.rating = rating;
  }
}
