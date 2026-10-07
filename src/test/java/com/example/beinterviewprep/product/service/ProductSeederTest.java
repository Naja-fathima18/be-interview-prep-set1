package com.example.beinterviewprep.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.product.domain.Product;
import com.example.beinterviewprep.product.persistence.ProductRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductSeederTest {

  private static final Instant NOW = Instant.parse("2026-10-07T09:00:00Z");

  @Mock private ProductRepository productRepository;

  @Captor private ArgumentCaptor<List<Product>> productsCaptor;

  private ProductSeeder seeder() {
    return new ProductSeeder(productRepository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void seedsRequestedNumberOfValidProductsWhenCatalogIsEmpty() {
    when(productRepository.count()).thenReturn(0L);

    int seeded = seeder().seedIfEmpty(100);

    assertThat(seeded).isEqualTo(100);
    verify(productRepository).saveAll(productsCaptor.capture());
    assertThat(productsCaptor.getValue())
        .hasSize(100)
        .allSatisfy(
            product -> {
              assertThat(product.getName()).isNotBlank();
              assertThat(product.getCategory()).isNotNull();
              assertThat(product.getPrice())
                  .isBetween(new BigDecimal("1.00"), new BigDecimal("1000.00"));
              assertThat(product.getStock()).isNotNegative();
              assertThat(product.getRating()).isBetween(BigDecimal.ONE, BigDecimal.valueOf(5));
              assertThat(product.getCreatedAt()).isBeforeOrEqualTo(NOW);
            });
  }

  @Test
  void skipsSeedingWhenCatalogAlreadyHasProducts() {
    when(productRepository.count()).thenReturn(3L);

    int seeded = seeder().seedIfEmpty(100);

    assertThat(seeded).isZero();
    verify(productRepository, never()).saveAll(any());
  }
}
