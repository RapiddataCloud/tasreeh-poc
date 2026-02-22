package com.tasreeh.po.order_service.repository;

import com.tasreeh.po.order_service.domain.OrderStatus;
import com.tasreeh.po.order_service.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {
    List<PurchaseOrder> findByUserIdOrderByCreatedAtDesc(String userId);
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();
    List<PurchaseOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}
