package com.laioffer.dispatchdeliveryapp.dto;

public record UpdateUserRequest(
        String name,
        String address,
        String email
) {}
