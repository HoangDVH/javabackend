package com.hoang.jwtjava.controller;

import com.hoang.jwtjava.dto.request.ProductReviewRequest;
import com.hoang.jwtjava.dto.response.ApiResponse;
import com.hoang.jwtjava.dto.response.PageResponse;
import com.hoang.jwtjava.dto.response.ProductReviewResponse;
import com.hoang.jwtjava.service.ProductReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

@Tag(name = "Reviews")
@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "List product reviews", description = "Public. Phân trang review theo sản phẩm (mới nhất trước).")
    public ResponseEntity<ApiResponse<PageResponse<ProductReviewResponse>>> list(
            @PathVariable Long productId,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<ProductReviewResponse>>builder()
                .result(productReviewService.listByProduct(productId, pageable))
                .build());
    }

    @PostMapping
    @Operation(summary = "Create review", description = """
            JWT required. Chỉ buyer đã mua sản phẩm (đơn PAID) được đánh giá.
            Mỗi user chỉ 1 review / sản phẩm. Rating 1–5; comment tùy chọn.
            Rating trung bình sản phẩm được cập nhật lại.
            """)
    public ResponseEntity<ApiResponse<ProductReviewResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long productId,
            @RequestBody @Valid ProductReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<ProductReviewResponse>builder()
                .message("Review created")
                .result(productReviewService.create(jwt.getSubject(), productId, request))
                .build());
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "Update my review", description = "Chỉ sửa review của chính mình.")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @RequestBody @Valid ProductReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.<ProductReviewResponse>builder()
                .message("Review updated")
                .result(productReviewService.update(jwt.getSubject(), productId, reviewId, request))
                .build());
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete review", description = "Xóa review của mình, hoặc ADMIN xóa bất kỳ.")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long productId,
            @PathVariable Long reviewId) {
        productReviewService.delete(jwt.getSubject(), hasRole(jwt, "ADMIN"), productId, reviewId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Review deleted")
                .build());
    }

    private static boolean hasRole(Jwt jwt, String role) {
        String scope = jwt.getClaimAsString("scope");
        if (scope == null || scope.isBlank())
            return false;
        for (String token : scope.split("\\s+")) {
            if (role.equalsIgnoreCase(token))
                return true;
        }
        return false;
    }
}
