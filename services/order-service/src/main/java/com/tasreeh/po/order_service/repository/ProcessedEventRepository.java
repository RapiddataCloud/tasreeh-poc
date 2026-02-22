package com.tasreeh.po.order_service.repository;

import com.tasreeh.po.order_service.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
