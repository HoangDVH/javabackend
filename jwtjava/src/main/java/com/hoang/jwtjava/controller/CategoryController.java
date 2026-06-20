package com.hoang.jwtjava.controller;

import com.hoang.jwtjava.dto.response.ApiResponse;
import com.hoang.jwtjava.dto.response.CategoryResponse;
import com.hoang.jwtjava.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Categories")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @SecurityRequirements
    @Operation(
            summary = "List all categories",
            description = """
                    Public endpoint. Trả toàn bộ danh mục, sắp xếp theo `id` ASC.
                    **Cache:** Redis TTL 30 phút (`catalog:categories:list`). Fail-open về DB nếu Redis không khả dụng.
                    """)
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.<List<CategoryResponse>>builder()
                .result(categoryService.listCategories())
                .build());
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(
            summary = "Get category by id",
            description = """
                    Public endpoint. **Cache:** Redis TTL 30 phút theo từng id (`catalog:category:id:{id}`).
                    """)
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(
            @Parameter(description = "Category id", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<CategoryResponse>builder()
                .result(categoryService.getCategory(id))
                .build());
    }
}
