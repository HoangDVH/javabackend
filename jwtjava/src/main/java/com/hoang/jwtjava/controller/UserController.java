package com.hoang.jwtjava.controller;

import com.hoang.jwtjava.dto.request.UserCreationRequest;
import com.hoang.jwtjava.dto.request.RoleAssignmentRequest;
import com.hoang.jwtjava.dto.request.UserUpdateRequest;
import com.hoang.jwtjava.dto.response.ApiResponse;
import com.hoang.jwtjava.dto.response.UserResponse;
import com.hoang.jwtjava.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<UserResponse>builder()
                        .result(userService.createUser(request))
                        .build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build());
    }

    @GetMapping("/me")
    @Operation(summary = "My profile", description = "Trả email, fullName, phone, roles.")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .result(userService.getUserByEmail(jwt.getSubject()))
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .result(userService.getUser(id))
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable String id,
                                                                @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(id, request))
                .build());
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @PathVariable String id,
            @RequestBody @Valid RoleAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .message("User role updated successfully")
                .result(userService.updateRole(id, request))
                .build());
    }

    @PutMapping("/me")
    @Operation(
            summary = "Update my profile",
            description = "Cập nhật tùy chọn: password và/hoặc fullName và/hoặc phone.")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyInfo(@AuthenticationPrincipal Jwt jwt,
                                                                   @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .result(userService.updateUserByEmail(jwt.getSubject(), request))
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("User deleted successfully")
                .build());
    }
}
