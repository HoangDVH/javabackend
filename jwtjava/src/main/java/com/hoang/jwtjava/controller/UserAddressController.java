package com.hoang.jwtjava.controller;

import com.hoang.jwtjava.dto.request.UserAddressRequest;
import com.hoang.jwtjava.dto.response.ApiResponse;
import com.hoang.jwtjava.dto.response.UserAddressResponse;
import com.hoang.jwtjava.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    @GetMapping
    @Operation(summary = "List my addresses", description = "Sổ địa chỉ của user đang đăng nhập (JWT).")
    public ResponseEntity<ApiResponse<List<UserAddressResponse>>> list(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.<List<UserAddressResponse>>builder()
                .result(userAddressService.listMyAddresses(jwt.getSubject()))
                .build());
    }

    @PostMapping
    @Operation(summary = "Create address", description = "Tạo địa chỉ. Nếu isDefault=true hoặc là địa chỉ đầu tiên thì trở thành mặc định.")
    public ResponseEntity<ApiResponse<UserAddressResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UserAddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<UserAddressResponse>builder()
                .message("Address created")
                .result(userAddressService.create(jwt.getSubject(), request))
                .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update address", description = "Cập nhật địa chỉ. isDefault=true sẽ bỏ mặc định các địa chỉ khác.")
    public ResponseEntity<ApiResponse<UserAddressResponse>> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody @Valid UserAddressRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserAddressResponse>builder()
                .message("Address updated")
                .result(userAddressService.update(jwt.getSubject(), id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete address")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        userAddressService.delete(jwt.getSubject(), id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Address deleted")
                .build());
    }
}
