package com.example.beinterviewprep.product.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.product.domain.Category;
import com.example.beinterviewprep.product.service.ProductFilter;
import com.example.beinterviewprep.product.service.ProductService;
import com.example.beinterviewprep.product.service.ProductView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

  private static final Instant CREATED_AT = Instant.parse("2026-10-07T09:00:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ProductService productService;

  @Test
  void listsProductsWithAllFiltersAndPageMetadata() throws Exception {
    when(productService.list(any(ProductFilter.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(view(7L)), PageRequest.of(1, 2), 5));

    mockMvc
        .perform(
            get("/api/products")
                .param("category", "ELECTRONICS")
                .param("minPrice", "10")
                .param("maxPrice", "250.50")
                .param("inStock", "true")
                .param("q", "smart")
                .param("page", "1")
                .param("size", "2")
                .param("sort", "price,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(7))
        .andExpect(jsonPath("$.content[0].name").value("Smart Speaker"))
        .andExpect(jsonPath("$.content[0].category").value("ELECTRONICS"))
        .andExpect(jsonPath("$.content[0].price").value(89.00))
        .andExpect(jsonPath("$.content[0].stock").value(3))
        .andExpect(jsonPath("$.content[0].rating").value(4.5))
        .andExpect(jsonPath("$.content[0].createdAt").value("2026-10-07T09:00:00Z"))
        .andExpect(jsonPath("$.totalElements").value(5))
        .andExpect(jsonPath("$.totalPages").value(3));

    ArgumentCaptor<ProductFilter> filter = ArgumentCaptor.forClass(ProductFilter.class);
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(productService).list(filter.capture(), pageable.capture());
    assertThat(filter.getValue())
        .isEqualTo(
            new ProductFilter(
                Category.ELECTRONICS,
                new BigDecimal("10"),
                new BigDecimal("250.50"),
                true,
                "smart"));
    assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
    assertThat(pageable.getValue().getPageSize()).isEqualTo(2);
    assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by("price"));
  }

  @Test
  void defaultsToNewestFirstWithoutFilters() throws Exception {
    when(productService.list(any(ProductFilter.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    mockMvc.perform(get("/api/products")).andExpect(status().isOk());

    ArgumentCaptor<ProductFilter> filter = ArgumentCaptor.forClass(ProductFilter.class);
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(productService).list(filter.capture(), pageable.capture());
    assertThat(filter.getValue()).isEqualTo(new ProductFilter(null, null, null, false, null));
    assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
  }

  @Test
  void capsPageSizeAtOneHundred() throws Exception {
    when(productService.list(any(ProductFilter.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    mockMvc.perform(get("/api/products").param("size", "500")).andExpect(status().isOk());

    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(productService).list(any(ProductFilter.class), pageable.capture());
    assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
  }

  @Test
  void rejectsInvertedPriceRange() throws Exception {
    mockMvc
        .perform(get("/api/products").param("minPrice", "100").param("maxPrice", "10"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.errors[0].field").value("priceRangeValid"))
        .andExpect(
            jsonPath("$.errors[0].message").value("minPrice must not be greater than maxPrice"));

    verifyNoInteractions(productService);
  }

  @Test
  void rejectsNegativePrice() throws Exception {
    mockMvc
        .perform(get("/api/products").param("minPrice", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("minPrice"))
        .andExpect(jsonPath("$.errors[0].message").value("Minimum price cannot be negative"));
  }

  @Test
  void rejectsUnknownCategory() throws Exception {
    mockMvc
        .perform(get("/api/products").param("category", "WEAPONS"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.errors[0].field").value("category"));

    verifyNoInteractions(productService);
  }

  private static ProductView view(Long id) {
    return new ProductView(
        id,
        "Smart Speaker",
        Category.ELECTRONICS,
        new BigDecimal("89.00"),
        3,
        new BigDecimal("4.5"),
        CREATED_AT);
  }
}
