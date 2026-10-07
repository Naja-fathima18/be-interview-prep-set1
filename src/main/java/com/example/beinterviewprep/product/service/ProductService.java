package com.example.beinterviewprep.product.service;

import static com.example.beinterviewprep.product.persistence.ProductSpecifications.hasCategory;
import static com.example.beinterviewprep.product.persistence.ProductSpecifications.inStock;
import static com.example.beinterviewprep.product.persistence.ProductSpecifications.nameContains;
import static com.example.beinterviewprep.product.persistence.ProductSpecifications.priceAtLeast;
import static com.example.beinterviewprep.product.persistence.ProductSpecifications.priceAtMost;

import com.example.beinterviewprep.common.error.ResourceNotFoundException;
import com.example.beinterviewprep.product.domain.Product;
import com.example.beinterviewprep.product.persistence.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

  public static final String PRODUCTS_CACHE = "products";
  private static final String TIE_BREAKER = "id";

  private final ProductRepository productRepository;
  private final Clock clock;

  public Page<ProductView> list(ProductFilter filter, Pageable pageable) {
    return productRepository
        .findAll(specificationFor(filter), withStableOrder(pageable))
        .map(ProductView::from);
  }

  @Cacheable(cacheNames = PRODUCTS_CACHE, key = "#id", sync = true)
  public ProductView get(Long id) {
    return ProductView.from(findProduct(id));
  }

  @Transactional
  public ProductView create(ProductCommand command) {
    Product product =
        productRepository.save(
            new Product(
                command.name(),
                command.category(),
                command.price(),
                command.stock(),
                command.rating(),
                Instant.now(clock)));
    log.info("Created product {}", product.getId());
    return ProductView.from(product);
  }

  @Transactional
  @CacheEvict(cacheNames = PRODUCTS_CACHE, key = "#id")
  public ProductView update(Long id, ProductCommand command) {
    Product product = findProduct(id);
    product.update(
        command.name(), command.category(), command.price(), command.stock(), command.rating());
    log.info("Updated product {}", id);
    return ProductView.from(product);
  }

  @Transactional
  @CacheEvict(cacheNames = PRODUCTS_CACHE, key = "#id")
  public void delete(Long id) {
    productRepository.delete(findProduct(id));
    log.info("Deleted product {}", id);
  }

  private Product findProduct(Long id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product", id));
  }

  private static Specification<Product> specificationFor(ProductFilter filter) {
    List<Specification<Product>> conditions = new ArrayList<>();
    if (filter.category() != null) {
      conditions.add(hasCategory(filter.category()));
    }
    if (filter.minPrice() != null) {
      conditions.add(priceAtLeast(filter.minPrice()));
    }
    if (filter.maxPrice() != null) {
      conditions.add(priceAtMost(filter.maxPrice()));
    }
    if (filter.inStockOnly()) {
      conditions.add(inStock());
    }
    if (filter.nameQuery() != null && !filter.nameQuery().isBlank()) {
      conditions.add(nameContains(filter.nameQuery().strip()));
    }
    return Specification.allOf(conditions);
  }

  private static Pageable withStableOrder(Pageable pageable) {
    Sort sort = pageable.getSort();
    if (sort.getOrderFor(TIE_BREAKER) != null) {
      return pageable;
    }
    return PageRequest.of(
        pageable.getPageNumber(), pageable.getPageSize(), sort.and(Sort.by(TIE_BREAKER)));
  }
}
