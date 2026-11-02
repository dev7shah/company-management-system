package com.company.cms.repository;

import com.company.cms.entity.OrderStatus;
import com.company.cms.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    Optional<SalesOrder> findByOrderNumber(String orderNumber);
    boolean existsByOrderNumber(String orderNumber);
    List<SalesOrder> findBySalesRepId(Long employeeId);
    List<SalesOrder> findByCustomerId(Long customerId);
    List<SalesOrder> findByStatus(OrderStatus status);
    List<SalesOrder> findByRegion(String region);
    List<SalesOrder> findByRegionIgnoreCase(String region);

    @Query("SELECT COUNT(i) FROM SalesOrderItem i WHERE i.product.id = :productId")
    long countOrderItemsByProductId(@Param("productId") Long productId);
}
