package com.laioffer.dispatchdeliveryapp.controller;

import com.laioffer.dispatchdeliveryapp.dto.CreateOrderRequest;
import com.laioffer.dispatchdeliveryapp.dto.OrderDetailResponse;
import com.laioffer.dispatchdeliveryapp.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/store")
public class StoreController {

    private final OrderService orderService;

    public StoreController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(Authentication authentication, @RequestBody CreateOrderRequest request) {
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
