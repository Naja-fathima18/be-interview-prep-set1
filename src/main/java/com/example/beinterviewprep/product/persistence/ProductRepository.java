package com.example.beinterviewprep.product.persistence;

import com.example.beinterviewprep.product.domain.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

  @Modifying(flushAutomatically = true)
  @Query(
      "update Product p set p.stock = p.stock - :quantity"
          + " where p.id = :id and p.stock >= :quantity")
  int decrementStock(Long id, int quantity);

  @Modifying(flushAutomatically = true)
  @Query("update Product p set p.stock = p.stock + :quantity where p.id = :id")
  int incrementStock(Long id, int quantity);

  @Query("select p.stock from Product p where p.id = :id")
  Optional<Integer> findStockById(Long id);
}
