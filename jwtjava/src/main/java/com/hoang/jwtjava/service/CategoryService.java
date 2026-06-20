package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.response.CategoryResponse;
import com.hoang.jwtjava.entity.Category;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CatalogCacheService catalogCacheService;

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        if (catalogCacheService.isActive()) {
            var cached = catalogCacheService.getCategoryList();
            if (cached.isPresent())
                return cached.get();
        }

        List<CategoryResponse> categories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toResponse)
                .toList();
        if (catalogCacheService.isActive())
            catalogCacheService.putCategoryList(categories);
        return categories;
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long id) {
        if (catalogCacheService.isActive()) {
            var cached = catalogCacheService.getCategory(id);
            if (cached.isPresent())
                return cached.get();
        }

        CategoryResponse response = toResponse(
                categoryRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND))
        );
        if (catalogCacheService.isActive())
            catalogCacheService.putCategory(id, response);
        return response;
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .name(c.getName())
                .build();
    }
}
