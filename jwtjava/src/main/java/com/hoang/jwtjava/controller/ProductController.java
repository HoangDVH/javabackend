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

import java.util.ArrayList;
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
                    description = "Một hoặc nhiều file ảnh (multipart).",
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))))
            @RequestPart("files") MultipartFile[] files) {
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
            **keyword:** tìm kiếm theo tên/mô tả sản phẩm (không phân biệt hoa thường).
            **Pagination:** use query params `page`, `size`, `sort` (e.g. `sort=createdAt,desc`). Do not use `string` as a sort property.
            """)
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> listProducts(
            @Parameter(description = "Filter by category id; omit = no filter")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by brand id; omit = no filter")
            @RequestParam(required = false) Long brandId,
            @Parameter(description = "Omit = all. `true` = featured only. `false` = non-featured only.")
            @RequestParam(required = false) Boolean isFeatured,
            @Parameter(description = "Search keyword in product name/description (case-insensitive).")
            @RequestParam(required = false) String keyword,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponse> page = productService.listProducts(categoryId, brandId, isFeatured, keyword, pageable);
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create product with images (single request)",
            description = "Gửi kèm JSON + ảnh multipart trong 1 request. Field `product` là JSON của ProductCreateRequest; field `files` là 1-n ảnh.")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProductWithImages(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "JSON ProductCreateRequest")
            @RequestPart("product") @Valid ProductCreateRequest request,
            @Parameter(
                    description = "Một hoặc nhiều file ảnh (multipart).",
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))))
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        List<String> merged = new ArrayList<>(request.getImages() != null ? request.getImages() : List.of());
        List<String> uploaded = saveUploadedFilesIfAny(files);
        merged.addAll(uploaded);
        request.setImages(merged);

        boolean isAdmin = hasRole(jwt, "ADMIN");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<ProductResponse>builder()
                        .message("Product created successfully")
                        .result(productService.createProduct(request, jwt.getSubject(), isAdmin))
                        .build());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
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

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update product with images (single request)",
            description = "Gửi kèm JSON + ảnh multipart trong 1 request. Field `product` là JSON của ProductCreateRequest; field `files` là 1-n ảnh (tuỳ chọn).")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductWithImages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Parameter(description = "JSON ProductCreateRequest")
            @RequestPart("product") @Valid ProductCreateRequest request,
            @Parameter(
                    description = "Một hoặc nhiều file ảnh (multipart).",
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))))
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        List<String> merged = new ArrayList<>(request.getImages() != null ? request.getImages() : List.of());
        List<String> uploaded = saveUploadedFilesIfAny(files);
        merged.addAll(uploaded);
        request.setImages(merged);

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

    private List<String> saveUploadedFilesIfAny(MultipartFile[] files) {
        if (files == null || files.length == 0)
            return List.of();
        List<MultipartFile> list = Arrays.asList(files);
        boolean anyNonEmpty = false;
        for (MultipartFile f : list) {
            if (f != null && !f.isEmpty()) {
                anyNonEmpty = true;
                break;
            }
        }
        if (!anyNonEmpty)
            return List.of();
        return productImageStorageService.saveUploadedFiles(list);
    }
}
