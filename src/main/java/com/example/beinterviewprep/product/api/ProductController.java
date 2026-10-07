package com.example.beinterviewprep.product.api;

import com.example.beinterviewprep.common.api.PageResponse;
import com.example.beinterviewprep.product.service.ProductService;
import com.example.beinterviewprep.product.service.ProductView;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  @GetMapping
  public PageResponse<ProductResponse> list(
      @Valid ProductSearchParams params,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return PageResponse.from(
        productService.list(params.toFilter(), pageable), ProductResponse::from);
  }

  @GetMapping("/{id}")
  public ProductResponse get(@PathVariable Long id) {
    return ProductResponse.from(productService.get(id));
  }

  @PostMapping
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
    ProductView product = productService.create(request.toCommand());
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(product.id())
            .toUri();
    return ResponseEntity.created(location).body(ProductResponse.from(product));
  }

  @PutMapping("/{id}")
  public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
    return ProductResponse.from(productService.update(id, request.toCommand()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    productService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
