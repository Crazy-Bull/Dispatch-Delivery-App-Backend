package com.laioffer.dispatchdeliveryapp.controller;

import com.laioffer.dispatchdeliveryapp.dto.AddUserRequest;
import com.laioffer.dispatchdeliveryapp.dto.UpdateUserRequest;
import com.laioffer.dispatchdeliveryapp.dto.UserResponse;
import com.laioffer.dispatchdeliveryapp.entity.Order;
import com.laioffer.dispatchdeliveryapp.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.getByEmail(email));
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getOrdersByUserId(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> addUser(@RequestBody AddUserRequest request) {
        UserResponse user = userService.addUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
