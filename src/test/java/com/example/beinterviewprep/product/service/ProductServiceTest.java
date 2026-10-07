package com.example.beinterviewprep.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.common.error.ResourceNotFoundException;
import com.example.beinterviewprep.product.domain.Category;
import com.example.beinterviewprep.product.domain.Product;
import com.example.beinterviewprep.product.persistence.ProductRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-07T09:00:00Z");
  private static final ProductCommand COMMAND =
      new ProductCommand(
          "Smart Lamp", Category.HOME, new BigDecimal("49.50"), 5, new BigDecimal("4.2"));

  @Mock private ProductRepository productRepository;

  private ProductService productService;

  @BeforeEach
  void setUp() {
    productService = new ProductService(productRepository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createsProductStampedWithCurrentTime() {
    when(productRepository.save(any(Product.class)))
        .thenAnswer(invocation -> withId(invocation.getArgument(0), 11L));

    ProductView created = productService.create(COMMAND);

    assertThat(created)
        .isEqualTo(
            new ProductView(
                11L,
                "Smart Lamp",
                Category.HOME,
                new BigDecimal("49.50"),
                5,
                new BigDecimal("4.2"),
                NOW));
  }

  @Test
  void returnsProductById() {
    when(productRepository.findById(3L)).thenReturn(Optional.of(existing(3L)));

    assertThat(productService.get(3L).name()).isEqualTo("Old Lamp");
  }

  @Test
  void throwsNotFoundForUnknownProduct() {
    when(productRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.get(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Product with id 99 was not found");
  }

  @Test
  void updatesEveryEditableFieldButKeepsCreationTime() {
    Product product = existing(3L);
    when(productRepository.findById(3L)).thenReturn(Optional.of(product));

    ProductView updated = productService.update(3L, COMMAND);

    assertThat(updated.name()).isEqualTo("Smart Lamp");
    assertThat(updated.category()).isEqualTo(Category.HOME);
    assertThat(updated.price()).isEqualByComparingTo("49.50");
    assertThat(updated.stock()).isEqualTo(5);
    assertThat(updated.rating()).isEqualByComparingTo("4.2");
    assertThat(updated.createdAt()).isEqualTo(NOW.minusSeconds(3600));
  }

  @Test
  void rejectsUpdateOfUnknownProduct() {
    when(productRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.update(99L, COMMAND))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deletesExistingProduct() {
    Product product = existing(3L);
    when(productRepository.findById(3L)).thenReturn(Optional.of(product));

    productService.delete(3L);

    verify(productRepository).delete(product);
  }

  @Test
  void rejectsDeleteOfUnknownProduct() {
    when(productRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.delete(99L))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(productRepository, never()).delete(any(Product.class));
  }

  private static Product existing(Long id) {
    return withId(
        new Product(
            "Old Lamp",
            Category.HOME,
            new BigDecimal("10.00"),
            1,
            new BigDecimal("3.0"),
            NOW.minusSeconds(3600)),
        id);
  }

  private static Product withId(Product product, Long id) {
    ReflectionTestUtils.setField(product, "id", id);
    return product;
  }
}
