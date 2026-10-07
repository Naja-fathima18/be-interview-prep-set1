package com.example.beinterviewprep.product.persistence;

import com.example.beinterviewprep.product.domain.Category;
import com.example.beinterviewprep.product.domain.Product;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

  private static final char LIKE_ESCAPE = '\\';

  private ProductSpecifications() {}

  public static Specification<Product> hasCategory(Category category) {
    return (root, query, cb) -> cb.equal(root.get("category"), category);
  }

  public static Specification<Product> priceAtLeast(BigDecimal minPrice) {
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
  }

  public static Specification<Product> priceAtMost(BigDecimal maxPrice) {
    return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
  }

  public static Specification<Product> inStock() {
    return (root, query, cb) -> cb.greaterThan(root.get("stock"), 0);
  }

  public static Specification<Product> nameContains(String text) {
    String pattern = "%" + escapeLikeWildcards(text.toLowerCase(Locale.ROOT)) + "%";
    return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE);
  }

  private static String escapeLikeWildcards(String text) {
    return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
