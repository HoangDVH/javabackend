package com.hoang.jwtjava.config;

import com.hoang.jwtjava.entity.Product;
import com.hoang.jwtjava.repository.ProductRepository;
import com.hoang.jwtjava.service.ProductImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Sửa sản phẩm đã seed trước đây (đường dẫn {@code catalog/...} hoặc danh sách ảnh trống):
 * tải ảnh mẫu tương ứng từ Picsum, lưu file storage và cập nhật URL trong DB.
 */
@Component
@Order(101)
@RequiredArgsConstructor
@Slf4j
public class ProductCatalogImageBackfill implements ApplicationRunner {

    @Value("${app.seed-images-from-network:true}")
    private boolean seedImagesFromNetwork;
    @Value("${app.migrate-legacy-local-images-on-startup:true}")
    private boolean migrateLegacyLocalImagesOnStartup;

    private final ProductRepository productRepository;
    private final ProductImageStorageService productImageStorageService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedImagesFromNetwork) {
            log.debug("Bỏ qua backfill ảnh catalog (app.seed-images-from-network=false).");
            return;
        }
        List<Product> all = productRepository.findAll();
        int fixed = 0;
        for (Product p : all) {
            if (!needsImageFix(p))
                continue;
            String base = "jwtjava-pid-" + p.getId();
            List<String> src = List.of(
                    "https://picsum.photos/seed/" + base + "-a/640/640.jpg",
                    "https://picsum.photos/seed/" + base + "-b/640/640.jpg"
            );
            try {
                p.setImages(productImageStorageService.importHttpImageUrlsToManagedStorage(src));
                productRepository.save(p);
                fixed++;
            } catch (Exception e) {
                log.warn("Không backfill ảnh sản phẩm id={}: {}", p.getId(), e.toString());
            }
        }
        if (fixed > 0)
            log.info("Đã gán ảnh lưu storage cho {} sản phẩm (trước đó catalog/trống).", fixed);
    }

    private boolean needsImageFix(Product p) {
        List<String> imgs = p.getImages();
        if (imgs == null || imgs.isEmpty())
            return true;
        for (String u : imgs) {
            if (u == null || u.isBlank())
                return true;
            if (u.contains("catalog/"))
                return true;
            if (isLegacyLocalImage(u))
                return true;
        }
        return false;
    }

    private boolean isLegacyLocalImage(String u) {
        if (!migrateLegacyLocalImagesOnStartup)
            return false;
        String s = u.trim();
        if (s.isBlank())
            return true;
        return s.startsWith("/files/product-images/")
                || s.contains("/files/product-images/")
                || s.startsWith("files/product-images/");
    }
}
