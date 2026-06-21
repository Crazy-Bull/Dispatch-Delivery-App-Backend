package com.laioffer.dispatchdeliveryapp.service;

import com.laioffer.dispatchdeliveryapp.dto.AddUserRequest;
import com.laioffer.dispatchdeliveryapp.dto.UpdateUserRequest;
import com.laioffer.dispatchdeliveryapp.entity.Order;
import com.laioffer.dispatchdeliveryapp.entity.User;
import com.laioffer.dispatchdeliveryapp.repository.OrderRepository;
import com.laioffer.dispatchdeliveryapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public UserService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + email));
    }

    public List<Order> getOrdersByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NoSuchElementException("User not found: " + userId);
        }
        return orderRepository.findByUserId(userId);
    }

    @Transactional
    public User addUser(AddUserRequest request) {
        validateUserInput(request.name(), request.address(), request.email());

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }

        return userRepository.save(new User(null, request.name(), request.address(), request.email()));
    }

    @Transactional
    public User updateUser(Long id, UpdateUserRequest request) {
        User existing = getById(id);
        validateUserInput(request.name(), request.address(), request.email());

        userRepository.findByEmail(request.email())
                .filter(user -> !user.id().equals(id))
                .ifPresent(user -> {
                    throw new IllegalArgumentException("Email already exists: " + request.email());
                });

        return userRepository.save(new User(id, request.name(), request.address(), request.email()));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NoSuchElementException("User not found: " + id);
        }
        if (orderRepository.existsByUserId(id)) {
            throw new IllegalStateException("Cannot delete user with existing orders: " + id);
        }
        userRepository.deleteById(id);
    }

    private void validateUserInput(String name, String address, String email) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
    }
}
