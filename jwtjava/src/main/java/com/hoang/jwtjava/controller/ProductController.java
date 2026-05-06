package com.hoang.jwtjava.controller;

import com.hoang.jwtjava.dto.request.ProductCreateRequest;
import com.hoang.jwtjava.dto.response.ApiResponse;
import com.hoang.jwtjava.dto.response.PageResponse;
import com.hoang.jwtjava.dto.response.ProductImageUploadResponse;
import com.hoang.jwtjava.dto.response.ProductResponse;
import com.hoang.jwtjava.service.ProductImageStorageService;
import com.hoang.jwtjava.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Tag(name = "Products")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductImageStorageService productImageStorageService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload product images", description =
            "Khi app.cloudinary.enabled=true: upload lên Cloudinary và trả list secure_url HTTPS. "
                    + "Khi tắt: file lưu local như trước và trả path/URL như storage.")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<ProductImageUploadResponse>> uploadProductImages(
            @Parameter(
                    description = "Một hoặc nhiều file ảnh (multipart). Trên Swagger: bấm **Choose File**, không dùng ô nhập text.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))))
            @RequestParam("files") MultipartFile[] files) {
        List<MultipartFile> list = files != null ? Arrays.asList(files) : List.of();
        ProductImageUploadResponse body = ProductImageUploadResponse.builder()
                .urls(productImageStorageService.saveUploadedFiles(list))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<ProductImageUploadResponse>builder()
                .message("Images uploaded successfully")
                .result(body)
                .build());
    }

    @GetMapping
    @Operation(summary = "List products (paginated)", description = """
            **isFeatured:** omit = *all* products. `true` = featured only. `false` = non-featured only.
            **Pagination:** use query params `page`, `size`, `sort` (e.g. `sort=createdAt,desc`). Do not use `string` as a sort property.
            """)
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> listProducts(
            @Parameter(description = "Filter by category id; omit = no filter")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by brand id; omit = no filter")
            @RequestParam(required = false) Long brandId,
            @Parameter(description = "Omit = all. `true` = featured only. `false` = non-featured only.")
            @RequestParam(required = false) Boolean isFeatured,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponse> page = productService.listProducts(categoryId, brandId, isFeatured, pageable);
        PageResponse<ProductResponse> body = PageResponse.<ProductResponse>builder()
                .items(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
        return ResponseEntity.ok(ApiResponse.<PageResponse<ProductResponse>>builder().result(body).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .result(productService.getProduct(id))
                .build());
    }

    @GetMapping("/seller/my")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getMySellerProducts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String sellerEmail) {
        String targetSeller = hasRole(jwt, "ADMIN") && sellerEmail != null && !sellerEmail.isBlank()
                ? sellerEmail
                : jwt.getSubject();
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .result(productService.getSellerProducts(targetSeller))
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ProductCreateRequest request) {
        boolean isAdmin = hasRole(jwt, "ADMIN");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<ProductResponse>builder()
                        .message("Product created successfully")
                        .result(productService.createProduct(request, jwt.getSubject(), isAdmin))
                        .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody @Valid ProductCreateRequest request) {
        boolean isAdmin = hasRole(jwt, "ADMIN");
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .message("Product updated successfully")
                .result(productService.updateProduct(id, request, jwt.getSubject(), isAdmin))
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        boolean isAdmin = hasRole(jwt, "ADMIN");
        productService.deleteProduct(id, jwt.getSubject(), isAdmin);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Product deleted successfully")
                .build());
    }

    private boolean hasRole(Jwt jwt, String role) {
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
