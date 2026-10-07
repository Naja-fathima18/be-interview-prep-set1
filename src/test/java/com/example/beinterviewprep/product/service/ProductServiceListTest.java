package com.example.beinterviewprep.product.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.beinterviewprep.TestcontainersConfiguration;
import com.example.beinterviewprep.common.config.ClockConfig;
import com.example.beinterviewprep.product.domain.Category;
import com.example.beinterviewprep.product.domain.Product;
import com.example.beinterviewprep.product.persistence.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, ClockConfig.class, ProductService.class})
class ProductServiceListTest {

  private static final Instant CREATED_AT = Instant.parse("2026-10-07T09:00:00Z");
  private static final ProductFilter NO_FILTER = new ProductFilter(null, null, null, false, null);

  @Autowired private ProductService productService;

  @Autowired private ProductRepository productRepository;

  @BeforeEach
  void setUp() {
    productRepository.saveAll(
        List.of(
            product("Smart Headphones", Category.ELECTRONICS, "199.99", 10, 0),
            product("Budget Headphones", Category.ELECTRONICS, "29.99", 0, 1),
            product("Smart Lamp", Category.HOME, "49.50", 5, 2),
            product("Smart Speaker", Category.ELECTRONICS, "89.00", 3, 3),
            product("Cooking Book", Category.BOOKS, "15.00", 40, 4),
            product("100% Cotton_Shirt", Category.CLOTHING, "25.00", 8, 5)));
  }

  @Test
  void returnsEverythingWithPageMetadataWhenNoFilterIsGiven() {
    Page<ProductView> page = productService.list(NO_FILTER, PageRequest.of(0, 4));

    assertThat(page.getTotalElements()).isEqualTo(6);
    assertThat(page.getTotalPages()).isEqualTo(2);
    assertThat(page.getContent()).hasSize(4);
  }

  @Test
  void combinesAllFiltersInOneQuery() {
    ProductFilter filter =
        new ProductFilter(
            Category.ELECTRONICS, new BigDecimal("50"), new BigDecimal("200"), true, "smart");

    Page<ProductView> page = productService.list(filter, PageRequest.of(0, 20, Sort.by("price")));

    assertThat(page.getContent())
        .extracting(ProductView::name)
        .containsExactly("Smart Speaker", "Smart Headphones");
  }

  @Test
  void filtersByCategoryOnly() {
    assertThat(names(new ProductFilter(Category.HOME, null, null, false, null)))
        .containsExactly("Smart Lamp");
  }

  @Test
  void filtersByInclusivePriceRange() {
    assertThat(
            names(
                new ProductFilter(
                    null, new BigDecimal("25.00"), new BigDecimal("49.50"), false, null)))
        .containsExactlyInAnyOrder("Budget Headphones", "Smart Lamp", "100% Cotton_Shirt");
  }

  @Test
  void excludesOutOfStockProductsWhenInStockOnly() {
    assertThat(names(new ProductFilter(Category.ELECTRONICS, null, null, true, null)))
        .containsExactlyInAnyOrder("Smart Headphones", "Smart Speaker");
  }

  @Test
  void searchesNameCaseInsensitively() {
    assertThat(names(new ProductFilter(null, null, null, false, "HEADPHONES")))
        .containsExactlyInAnyOrder("Smart Headphones", "Budget Headphones");
  }

  @Test
  void treatsLikeWildcardsInSearchTextLiterally() {
    assertThat(names(new ProductFilter(null, null, null, false, "100%")))
        .containsExactly("100% Cotton_Shirt");
    assertThat(names(new ProductFilter(null, null, null, false, "o_t"))).isEmpty();
  }

  @Test
  void sortsByAnyFieldWithIdAsTieBreaker() {
    Page<ProductView> byStockDesc =
        productService.list(
            NO_FILTER, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "stock")));
    Page<ProductView> byCategory =
        productService.list(NO_FILTER, PageRequest.of(0, 20, Sort.by("category")));

    assertThat(byStockDesc.getContent())
        .extracting(ProductView::stock)
        .isSortedAccordingTo((a, b) -> Integer.compare(b, a));
    List<ProductView> electronics =
        byCategory.getContent().stream()
            .filter(product -> product.category() == Category.ELECTRONICS)
            .toList();
    assertThat(electronics).extracting(ProductView::id).isSorted();
  }

  private List<String> names(ProductFilter filter) {
    return productService.list(filter, PageRequest.of(0, 20)).getContent().stream()
        .map(ProductView::name)
        .toList();
  }

  private static Product product(
      String name, Category category, String price, int stock, int ageInDays) {
    return new Product(
        name,
        category,
        new BigDecimal(price),
        stock,
        new BigDecimal("4.0"),
        CREATED_AT.minusSeconds(ageInDays * 86_400L));
  }
}
