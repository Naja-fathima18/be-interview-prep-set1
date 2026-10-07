package com.example.beinterviewprep.product.service;

import com.example.beinterviewprep.product.domain.Category;
import com.example.beinterviewprep.product.domain.Product;
import com.example.beinterviewprep.product.persistence.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSeeder {

  private static final long RANDOM_SEED = 42L;
  private static final List<String> ADJECTIVES =
      List.of("Classic", "Compact", "Deluxe", "Eco", "Essential", "Pro", "Smart", "Ultra");
  private static final List<String> MODELS = List.of("Mini", "Plus", "Max", "Lite", "One");

  private final ProductRepository productRepository;
  private final Clock clock;

  @Transactional
  public int seedIfEmpty(int count) {
    if (productRepository.count() > 0) {
      log.info("Product catalog already populated, skipping seed");
      return 0;
    }
    Random random = new Random(RANDOM_SEED);
    Instant now = Instant.now(clock);
    List<Product> products =
        IntStream.rangeClosed(1, count).mapToObj(i -> randomProduct(i, random, now)).toList();
    productRepository.saveAll(products);
    log.info("Seeded {} products", products.size());
    return products.size();
  }

  private static Product randomProduct(int number, Random random, Instant now) {
    Category category = pick(List.of(Category.values()), random);
    String name =
        "%s %s %s %d"
            .formatted(pick(ADJECTIVES, random), noun(category), pick(MODELS, random), number);
    BigDecimal price =
        BigDecimal.valueOf(1 + random.nextDouble() * 999).setScale(2, RoundingMode.HALF_UP);
    int stock = random.nextInt(5) == 0 ? 0 : random.nextInt(1, 500);
    BigDecimal rating =
        BigDecimal.valueOf(1 + random.nextDouble() * 4).setScale(1, RoundingMode.HALF_UP);
    Instant createdAt =
        now.minus(Duration.ofMinutes(random.nextLong(Duration.ofDays(365).toMinutes())));
    return new Product(name, category, price, stock, rating, createdAt);
  }

  private static String noun(Category category) {
    return switch (category) {
      case ELECTRONICS -> "Headphones";
      case BOOKS -> "Notebook";
      case CLOTHING -> "Jacket";
      case HOME -> "Lamp";
      case SPORTS -> "Yoga Mat";
      case TOYS -> "Puzzle";
      case GROCERY -> "Coffee";
      case BEAUTY -> "Moisturiser";
    };
  }

  private static <T> T pick(List<T> options, Random random) {
    return options.get(random.nextInt(options.size()));
  }
}
