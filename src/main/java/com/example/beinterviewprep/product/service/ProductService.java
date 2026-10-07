package com.example.beinterviewprep.product.service;

import static com.example.beinterviewprep.product.persistence.ProductSpecifications.hasCategory;
import static com.example.beinterviewprep.product.persistence.ProductSpecifications.inStock;
import static com.example.beinterviewprep.product.persistence.ProductSpecifications.nameContains;
import static com.example.beinterviewprep.product.persistence.ProductSpecifications.priceAtLeast;
import static com.example.beinterviewprep.product.persistence.ProductSpecifications.priceAtMost;

import com.example.beinterviewprep.product.domain.Product;
import com.example.beinterviewprep.product.persistence.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

  private static final String TIE_BREAKER = "id";

  private final ProductRepository productRepository;

  public Page<ProductView> list(ProductFilter filter, Pageable pageable) {
    return productRepository
        .findAll(specificationFor(filter), withStableOrder(pageable))
        .map(ProductView::from);
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
