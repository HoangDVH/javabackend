package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.request.RoleAssignmentRequest;
import com.hoang.jwtjava.dto.request.UserCreationRequest;
import com.hoang.jwtjava.dto.request.UserUpdateRequest;
import com.hoang.jwtjava.dto.response.UserResponse;
import com.hoang.jwtjava.entity.User;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.mapper.UserMapper;
import com.hoang.jwtjava.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse createUser(UserCreationRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new AppException(ErrorCode.USER_EXISTED);

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of("USER"));

        return userMapper.toUserResponse(userRepository.save(user));
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    public UserResponse getUser(String id) {
        return userMapper.toUserResponse(
                userRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND))
        );
    }

    public UserResponse getUserByEmail(String email) {
        return userMapper.toUserResponse(
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND))
        );
    }

    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return doUpdate(user, request);
    }

    public UserResponse updateUserByEmail(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return doUpdate(user, request);
    }

    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    public UserResponse updateRole(String userId, RoleAssignmentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String role = normalizeRole(request.getRole());
        if (!Set.of("USER", "SELLER", "ADMIN").contains(role))
            throw new AppException(ErrorCode.ROLE_INVALID);

        Set<String> roles = user.getRoles() != null ? new TreeSet<>(user.getRoles()) : new TreeSet<>();
        if (request.isEnabled())
            roles.add(role);
        else
            roles.remove(role);
        if (roles.isEmpty())
            roles.add("USER");
        user.setRoles(roles);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    private UserResponse doUpdate(User user, UserUpdateRequest request) {
        if (request.getPassword() != null && !request.getPassword().isBlank())
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        return userMapper.toUserResponse(userRepository.save(user));
    }

    private String normalizeRole(String role) {
        if (role == null)
            throw new AppException(ErrorCode.ROLE_INVALID);
        return role.trim().toUpperCase(Locale.ROOT);
    }
}
