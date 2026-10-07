package com.example.beinterviewprep.product.service;

import static com.example.beinterviewprep.product.service.ProductService.PRODUCTS_CACHE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beinterviewprep.common.config.CacheConfig;
import com.example.beinterviewprep.common.config.CachingProperties;
import com.example.beinterviewprep.common.error.ResourceNotFoundException;
import com.example.beinterviewprep.product.domain.Category;
import com.example.beinterviewprep.product.domain.Product;
import com.example.beinterviewprep.product.persistence.ProductRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(ProductCachingTest.Config.class)
class ProductCachingTest {

  private static final Long ID = 7L;
  private static final Instant NOW = Instant.parse("2026-10-07T09:00:00Z");

  @MockitoBean private ProductRepository productRepository;

  @Autowired private ProductService productService;

  @Autowired private CacheManager cacheManager;

  @Autowired private RecordingTransactionManager transactionManager;

  private Cache cache;

  @BeforeEach
  void setUp() {
    cache = cacheManager.getCache(PRODUCTS_CACHE);
    cache.clear();
    transactionManager.begun.set(0);
    when(productRepository.findById(ID)).thenReturn(Optional.of(product("Smart Lamp")));
  }

  @Test
  void servesRepeatedLookupsFromCacheWithoutQueryingTheDatabase() {
    for (int i = 0; i < 5; i++) {
      assertThat(productService.get(ID).name()).isEqualTo("Smart Lamp");
    }

    verify(productRepository, times(1)).findById(ID);
    assertThat(transactionManager.begun).hasValue(1);
  }

  @Test
  void returnsUpdatedProductOnTheNextLookup() {
    productService.get(ID);

    productService.update(
        ID,
        new ProductCommand(
            "Smart Lamp v2", Category.HOME, new BigDecimal("59.00"), 4, new BigDecimal("4.8")));

    ProductView afterUpdate = productService.get(ID);
    assertThat(afterUpdate.name()).isEqualTo("Smart Lamp v2");
    assertThat(afterUpdate.price()).isEqualByComparingTo("59.00");
  }

  @Test
  void returnsNotFoundOnTheNextLookupAfterDelete() {
    productService.get(ID);

    productService.delete(ID);
    when(productRepository.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> productService.get(ID)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void evictsOnlyOnceTheChangeIsCommitted() {
    productService.get(ID);
    TransactionTemplate outerTransaction = new TransactionTemplate(transactionManager);

    outerTransaction.executeWithoutResult(
        status -> {
          productService.update(
              ID,
              new ProductCommand(
                  "Smart Lamp v2", Category.HOME, new BigDecimal("59.00"), 4, BigDecimal.ONE));
          assertThat(cache.get(ID)).isNotNull();
        });

    assertThat(cache.get(ID)).isNull();
  }

  @Test
  void doesNotCacheMissingProducts() {
    when(productRepository.findById(99L)).thenReturn(Optional.empty());
    clearInvocations(productRepository);

    assertThatThrownBy(() -> productService.get(99L)).isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> productService.get(99L)).isInstanceOf(ResourceNotFoundException.class);

    verify(productRepository, times(2)).findById(99L);
  }

  private static Product product(String name) {
    Product product =
        new Product(name, Category.HOME, new BigDecimal("49.50"), 5, new BigDecimal("4.2"), NOW);
    ReflectionTestUtils.setField(product, "id", ID);
    return product;
  }

  @Configuration
  @EnableTransactionManagement
  @EnableConfigurationProperties(CachingProperties.class)
  @Import({CacheConfig.class, ProductService.class})
  static class Config {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Bean
    RecordingTransactionManager transactionManager() {
      return new RecordingTransactionManager();
    }
  }

  static class RecordingTransactionManager extends AbstractPlatformTransactionManager {

    final AtomicInteger begun = new AtomicInteger();

    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) {
      return TransactionSynchronizationManager.isActualTransactionActive();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
      begun.incrementAndGet();
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {}

    @Override
    protected void doRollback(DefaultTransactionStatus status) {}
  }
}
