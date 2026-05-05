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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductImageUploadResponse>> uploadProductImages(
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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .result(productService.getProduct(id))
                .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@RequestBody @Valid ProductCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<ProductResponse>builder()
                        .message("Product created successfully")
                        .result(productService.createProduct(request))
                        .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid ProductCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .message("Product updated successfully")
                .result(productService.updateProduct(id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Product deleted successfully")
                .build());
    }
}
