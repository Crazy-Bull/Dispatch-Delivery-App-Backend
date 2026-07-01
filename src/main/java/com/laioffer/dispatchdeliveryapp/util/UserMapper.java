package com.laioffer.dispatchdeliveryapp.util;

import com.laioffer.dispatchdeliveryapp.dto.UserResponse;
import com.laioffer.dispatchdeliveryapp.entity.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.id(), user.name(), user.address(), user.email());
    }
}
