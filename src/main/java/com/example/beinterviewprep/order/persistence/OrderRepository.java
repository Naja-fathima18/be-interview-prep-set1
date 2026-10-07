package com.example.beinterviewprep.order.persistence;

import com.example.beinterviewprep.order.domain.Order;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

  @EntityGraph(attributePaths = "items")
  Optional<Order> findByIdAndCustomerId(Long id, Long customerId);

  @EntityGraph(attributePaths = "items")
  Optional<Order> findByCustomerIdAndIdempotencyKey(Long customerId, String idempotencyKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Order o where o.id = :id and o.customerId = :customerId")
  Optional<Order> findForUpdate(Long id, Long customerId);
}
