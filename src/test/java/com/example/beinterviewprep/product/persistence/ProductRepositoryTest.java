package com.example.beinterviewprep.product.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.beinterviewprep.TestcontainersConfiguration;
import com.example.beinterviewprep.product.domain.Category;
import com.example.beinterviewprep.product.domain.Product;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {

  private static final Instant CREATED_AT = Instant.parse("2026-10-07T09:00:00Z");

  @Autowired private ProductRepository productRepository;

  @Autowired private TestEntityManager entityManager;

  @Test
  void persistsAllProductFields() {
    Product saved =
        productRepository.saveAndFlush(
            new Product(
                "Noise cancelling headphones",
                Category.ELECTRONICS,
                new BigDecimal("199.99"),
                12,
                new BigDecimal("4.6"),
                CREATED_AT));
    entityManager.clear();

    Product found = productRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getName()).isEqualTo("Noise cancelling headphones");
    assertThat(found.getCategory()).isEqualTo(Category.ELECTRONICS);
    assertThat(found.getPrice()).isEqualByComparingTo("199.99");
    assertThat(found.getStock()).isEqualTo(12);
    assertThat(found.getRating()).isEqualByComparingTo("4.6");
    assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
  }
}
