package com.example.beinterviewprep.product.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.beinterviewprep.common.config.ClockConfig;
import com.example.beinterviewprep.common.error.ResourceNotFoundException;
import com.example.beinterviewprep.common.security.SecurityConfig;
import com.example.beinterviewprep.product.domain.Category;
import com.example.beinterviewprep.product.service.ProductCommand;
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
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, ClockConfig.class})
@WithMockUser
class ProductControllerTest {

  private static final Instant CREATED_AT = Instant.parse("2026-10-07T09:00:00Z");
  private static final String VALID_BODY =
      """
      {"name": "Smart Speaker", "category": "ELECTRONICS", "price": 89.00, "stock": 3, "rating": 4.5}
      """;

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

  @Test
  void returnsProductById() throws Exception {
    when(productService.get(7L)).thenReturn(view(7L));

    mockMvc
        .perform(get("/api/products/7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.name").value("Smart Speaker"));
  }

  @Test
  void returnsNotFoundProblemForUnknownProduct() throws Exception {
    when(productService.get(99L)).thenThrow(new ResourceNotFoundException("Product", 99L));

    mockMvc
        .perform(get("/api/products/99"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Product with id 99 was not found"));
  }

  @Test
  void createsProductAndReturnsLocation() throws Exception {
    when(productService.create(any(ProductCommand.class))).thenReturn(view(7L));

    mockMvc
        .perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/api/products/7"))
        .andExpect(jsonPath("$.id").value(7));

    verify(productService)
        .create(
            new ProductCommand(
                "Smart Speaker",
                Category.ELECTRONICS,
                new BigDecimal("89.00"),
                3,
                new BigDecimal("4.5")));
  }

  @Test
  void rejectsInvalidProductWithFieldLevelMessages() throws Exception {
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": " ", "price": -1, "stock": -2, "rating": 5.5}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(
            jsonPath(
                "$.errors[*].field", hasItems("category", "name", "price", "rating", "stock")));

    verifyNoInteractions(productService);
  }

  @Test
  void updatesProduct() throws Exception {
    when(productService.update(eq(7L), any(ProductCommand.class))).thenReturn(view(7L));

    mockMvc
        .perform(put("/api/products/7").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7));
  }

  @Test
  void returnsNotFoundWhenUpdatingUnknownProduct() throws Exception {
    when(productService.update(eq(99L), any(ProductCommand.class)))
        .thenThrow(new ResourceNotFoundException("Product", 99L));

    mockMvc
        .perform(
            put("/api/products/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isNotFound());
  }

  @Test
  void deletesProduct() throws Exception {
    mockMvc.perform(delete("/api/products/7")).andExpect(status().isNoContent());

    verify(productService).delete(7L);
  }

  @Test
  void returnsNotFoundWhenDeletingUnknownProduct() throws Exception {
    doThrow(new ResourceNotFoundException("Product", 99L)).when(productService).delete(99L);

    mockMvc.perform(delete("/api/products/99")).andExpect(status().isNotFound());
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
