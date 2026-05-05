package com.hoang.jwtjava.mapper;

import com.hoang.jwtjava.dto.request.UserCreationRequest;
import com.hoang.jwtjava.dto.response.UserResponse;
import com.hoang.jwtjava.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toUser(UserCreationRequest request) {
        if (request == null) return null;
        return User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
    }

    public UserResponse toUserResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();
    }
}
