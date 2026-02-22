package com.tasreeh.po.order_service.repository;

import com.tasreeh.po.order_service.domain.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderEventRepository extends JpaRepository<OrderEvent, UUID> {

    List<OrderEvent> findByOrderIdOrderByTimestampAsc(UUID orderId);
}
