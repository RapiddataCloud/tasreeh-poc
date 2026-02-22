package com.tasreeh.po.order_service.api;

import com.tasreeh.po.order_service.api.dto.CreateOrderRequest;
import com.tasreeh.po.order_service.api.dto.OrderResponse;
import com.tasreeh.po.order_service.api.dto.RejectRequest;
import com.tasreeh.po.order_service.domain.OrderEvent;
import com.tasreeh.po.order_service.domain.OrderStatus;
import com.tasreeh.po.order_service.domain.PurchaseOrder;
import com.tasreeh.po.order_service.service.EventStore;
import com.tasreeh.po.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final EventStore eventStore;

    /**
     * Extract userId (sub claim) from JWT Bearer token in Authorization header.
     * KrakenD CE doesn't forward JWT claims as custom headers, so we parse the token ourselves.
     */
    private String extractUserIdFromAuth(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
                    // Extract "sub" claim
                    String sub = extractJsonString(payloadJson, "sub");
                    if (sub != null && !sub.isBlank()) {
                        log.info("[ORDER-CONTROLLER] Extracted userId from JWT: {}", sub);
                        return sub;
                    }
                }
            } catch (Exception e) {
                log.warn("[ORDER-CONTROLLER] Failed to parse JWT from Authorization header", e);
            }
        }
        log.warn("[ORDER-CONTROLLER] No valid Authorization header, falling back to 'unknown-user'");
        return "unknown-user";
    }

    /** Simple JSON string value extractor */
    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx < 0) return null;

        int colonIdx = json.indexOf(":", keyIdx + searchKey.length());
        if (colonIdx < 0) return null;

        int startQuote = json.indexOf("\"", colonIdx + 1);
        if (startQuote < 0) return null;

        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote < 0) return null;

        return json.substring(startQuote + 1, endQuote);
    }

    @PostMapping
    public OrderResponse create(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody CreateOrderRequest req) {

        String userId = extractUserIdFromAuth(authHeader);
        PurchaseOrder order = orderService.createOrder(userId, req);
        return toResponse(order);
    }

    /** Get all orders */
    @GetMapping
    public List<OrderResponse> allOrders() {
        return orderService.getAllOrders().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Employee: Get my orders */
    @GetMapping("/my")
    public List<OrderResponse> myOrders(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String userId = extractUserIdFromAuth(authHeader);
        return orderService.getOrdersForUser(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Manager: Get pending orders */
    @GetMapping("/pending")
    public List<OrderResponse> pendingOrders() {
        return orderService.getOrdersByStatus(OrderStatus.PENDING_APPROVAL).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Manager: Approve an order */
    @PutMapping("/{id}/approve")
    public ResponseEntity<OrderResponse> approveOrder(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String managerId = extractUserIdFromAuth(authHeader);
        PurchaseOrder order = orderService.approveOrder(id, managerId);
        return ResponseEntity.ok(toResponse(order));
    }

    /** Manager: Reject an order */
    @PutMapping("/{id}/reject")
    public ResponseEntity<OrderResponse> rejectOrder(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) RejectRequest body) {

        String managerId = extractUserIdFromAuth(authHeader);
        String reason = body != null ? body.getReason() : "Rejected by manager";
        PurchaseOrder order = orderService.rejectOrder(id, managerId, reason);
        return ResponseEntity.ok(toResponse(order));
    }

    /** Event Sourcing: Get the full event log for an order */
    @GetMapping("/{id}/events")
    public List<OrderEvent> getOrderEvents(@PathVariable UUID id) {
        return eventStore.getAllEvents(id);
    }

    /** Event Sourcing: Derive current status by replaying events */
    @GetMapping("/{id}/replay")
    public ResponseEntity<String> replayOrderState(@PathVariable UUID id) {
        OrderStatus status = eventStore.replayToCurrentStatus(id);
        if (status == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(String.format("{\"orderId\":\"%s\",\"derivedStatus\":\"%s\"}", id, status));
    }

    private OrderResponse toResponse(PurchaseOrder o) {
        return OrderResponse.builder()
                .id(o.getId())
                .item(o.getItem())
                .amount(o.getAmount())
                .description(o.getDescription())
                .status(o.getStatus().name())
                .submittedBy(o.getUserId())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
