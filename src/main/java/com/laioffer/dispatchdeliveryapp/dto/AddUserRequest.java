package com.laioffer.dispatchdeliveryapp.dto;

public record AddUserRequest(
        String name,
        String address,
        String email,
        String password
) {}
