package com.example.beinterviewprep.order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customer_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "customer_id", nullable = false, updatable = false)
  private Long customerId;

  @Column(name = "idempotency_key", nullable = false, updatable = false, length = 100)
  private String idempotencyKey;

  @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
  private String requestHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrderStatus status;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal total;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("productId")
  private List<OrderItem> items = new ArrayList<>();

  public Order(Long customerId, String idempotencyKey, String requestHash, Instant createdAt) {
    this.customerId = customerId;
    this.idempotencyKey = idempotencyKey;
    this.requestHash = requestHash;
    this.status = OrderStatus.PLACED;
    this.total = BigDecimal.ZERO;
    this.createdAt = createdAt;
  }

  public void addItem(Long productId, int quantity, BigDecimal unitPrice) {
    OrderItem item = new OrderItem(this, productId, quantity, unitPrice);
    items.add(item);
    total = total.add(item.lineTotal());
  }

  public boolean cancel(Instant now) {
    if (status == OrderStatus.CANCELLED) {
      return false;
    }
    status = OrderStatus.CANCELLED;
    cancelledAt = now;
    return true;
  }

  public boolean matchesRequest(String requestHash) {
    return this.requestHash.equals(requestHash);
  }

  public List<OrderItem> getItems() {
    return Collections.unmodifiableList(items);
  }
}
