package com.laioffer.dispatchdeliveryapp.controller;

import com.laioffer.dispatchdeliveryapp.dto.CreateOrderRequest;
import com.laioffer.dispatchdeliveryapp.dto.OrderDetailResponse;
import com.laioffer.dispatchdeliveryapp.dto.OrderPlansRequest;
import com.laioffer.dispatchdeliveryapp.dto.OrderTrackingResponse;
import com.laioffer.dispatchdeliveryapp.entity.Order;
import com.laioffer.dispatchdeliveryapp.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<OrderTrackingResponse> getOrderTracking(
            Authentication authentication,
            @PathVariable Long id) {
        try {
            Long userId = (Long) authentication.getPrincipal();
            return ResponseEntity.ok(orderService.getOrderTracking(id, userId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponse> getOrder(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(orderService.getOrderDetail(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Order>> getOrdersByUser(@RequestParam("user_id") Long userId) {
        try {
            return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/plans")
    public ResponseEntity<?> getDeliveryPlans(@RequestBody OrderPlansRequest request) {
        try {
            return ResponseEntity.ok(orderService.getDeliveryPlans(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createOrder(Authentication authentication, @RequestBody CreateOrderRequest request) {
        try {
            Long userId = (Long) authentication.getPrincipal();
            OrderDetailResponse response = orderService.createOrder(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
