package com.hoang.jwtjava.config;

import com.hoang.jwtjava.entity.Category;
import com.hoang.jwtjava.entity.Product;
import com.hoang.jwtjava.repository.CategoryRepository;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seed 6 danh mục (Điện thoại, Áo, Quần, Giày, Laptop, Nón) và 50 sản phẩm mẫu khi DB trống.
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class EcommerceDataSeeder implements ApplicationRunner {

    @Value("${app.seed-images-from-network:true}")
    private boolean seedImagesFromNetwork;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageStorageService productImageStorageService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategoriesIfEmpty();
        seedProductsIfEmpty();
    }

    private void seedCategoriesIfEmpty() {
        if (categoryRepository.count() > 0)
            return;
        List<Category> categories = List.of(
                Category.builder().code("PHONE").name("Điện thoại").build(),
                Category.builder().code("SHIRT").name("Áo").build(),
                Category.builder().code("PANTS").name("Quần").build(),
                Category.builder().code("SHOES").name("Giày").build(),
                Category.builder().code("LAPTOP").name("Laptop").build(),
                Category.builder().code("HAT").name("Nón").build()
        );
        categoryRepository.saveAll(categories);
        log.info("Seeded {} product categories", categories.size());
    }

    private void seedProductsIfEmpty() {
        long n = productRepository.count();
        if (n > 0) {
            log.info("Bỏ qua seed 50 sản phẩm mẫu: database đã có {} sản phẩm (chỉ seed khi count = 0).", n);
            return;
        }
        Map<String, Category> byCode = new LinkedHashMap<>();
        for (Category c : categoryRepository.findAll())
            byCode.put(c.getCode(), c);

        List<Product> products = new ArrayList<>();
        products.addAll(buildPhones(byCode.get("PHONE")));
        products.addAll(buildShirts(byCode.get("SHIRT")));
        products.addAll(buildPants(byCode.get("PANTS")));
        products.addAll(buildShoes(byCode.get("SHOES")));
        products.addAll(buildLaptops(byCode.get("LAPTOP")));
        products.addAll(buildHats(byCode.get("HAT")));

        if (seedImagesFromNetwork) {
            for (Product p : products) {
                p.setImages(productImageStorageService.resolveImageUrlsForPersistence(p.getImages()));
            }
        }
        productRepository.saveAll(products);
        log.info("Seeded {} sample products{}", products.size(),
                seedImagesFromNetwork ? " (ảnh đã tải về storage)" : " (giữ URL Picsum, chưa tải — app.seed-images-from-network=false)");
    }

    private static Product product(
            Category category,
            String name,
            String description,
            int price,
            int discountPrice,
            int stock,
            long brandId,
            double rating,
            boolean featured,
            String imageKey
    ) {
        return Product.builder()
                .name(name)
                .description(description)
                .price(price)
                .discountPrice(discountPrice)
                .stock(stock)
                .category(category)
                .brandId(brandId)
                .images(new ArrayList<>(List.of(
                        "https://picsum.photos/seed/jwtjava-" + imageKey + "-a/640/640.jpg",
                        "https://picsum.photos/seed/jwtjava-" + imageKey + "-b/640/640.jpg"
                )))
                .rating(BigDecimal.valueOf(rating).setScale(2, RoundingMode.HALF_UP))
                .featured(featured)
                .build();
    }

    private List<Product> buildPhones(Category cat) {
        String[][] rows = {
                {"iPhone 16 Pro", "Flagship Apple, chip A18 Pro, camera 48MP.", "32990000", "30990000", "42", "1"},
                {"Samsung Galaxy S24 Ultra", "S Pen tích hợp, màn Dynamic AMOLED 2X 6.8 inch.", "28990000", "26990000", "35", "2"},
                {"Xiaomi 14", "Leica lens, Snapdragon 8 Gen 3, sạc nhanh 90W.", "16990000", "15490000", "50", "3"},
                {"OPPO Reno11", "Chân dung tele, thiết kế mỏng nhẹ.", "10990000", "9990000", "28", "4"},
                {"Google Pixel 8", "Android gốc, AI Magic Eraser, chụp đêm.", "18990000", "17490000", "22", "5"},
                {"vivo V30", "Đèn flash Aura Light, pin 5000mAh.", "9990000", "8990000", "45", "1"},
                {"realme 12 Pro+", "Camera periscope giá tốt, màn 120Hz.", "11990000", "10490000", "33", "2"},
                {"Nokia G42 5G", "5G bền bỉ, cập nhật phần mềm dài hạn.", "4490000", "3990000", "60", "3"},
                {"Nothing Phone (2a)", "Glyph Interface, thiết kế trong suốt.", "7990000", "7290000", "40", "4"}
        };
        return mapRows(cat, rows, "PHONE");
    }

    private List<Product> buildShirts(Category cat) {
        String[][] rows = {
                {"Áo thun nam cổ trụ", "Cotton 100%, thấm hút, nhiều màu.", "249000", "199000", "120", "1"},
                {"Áo sơ mi trắng công sở", "Form regular, dễ ủi, công sở hàng ngày.", "399000", "329000", "85", "2"},
                {"Áo polo basic", "Pique cotton, cổ bẻ thanh lịch.", "299000", "249000", "95", "3"},
                {"Áo hoodie unisex", "Nỉ bông dày, túi kangaroo.", "459000", "399000", "70", "4"},
                {"Áo len cổ lọ", "Giữ ấm, phối layer mùa lạnh.", "529000", "449000", "55", "5"},
                {"Áo khoác jean", "Wash nhẹ, túi ngực.", "599000", "499000", "48", "1"},
                {"Áo vest nam", "Tweed xám, dự tiệc.", "1299000", "1099000", "30", "2"},
                {"Áo flannel kẻ sọc", "Flannel mềm, phong cách casual.", "379000", "319000", "65", "3"}
        };
        return mapRows(cat, rows, "SHIRT");
    }

    private List<Product> buildPants(Category cat) {
        String[][] rows = {
                {"Quần jean slim fit", "Denim co giãn nhẹ, cạp vừa.", "599000", "499000", "90", "1"},
                {"Quần tây nam ống đứng", "Vải tuytsi, form công sở.", "449000", "379000", "75", "2"},
                {"Quần jogger thể thao", "Poly-spandex, bo gấu.", "329000", "279000", "110", "3"},
                {"Quần short kaki", "Đi biển, đi phố.", "199000", "159000", "130", "4"},
                {"Quần legging nữ", "Tập gym / yoga, co giãn 4 chiều.", "259000", "219000", "100", "5"},
                {"Quần âu cạp cao", "Hack dáng chân dài.", "489000", "419000", "62", "1"},
                {"Quần cargo túi hộp", "Streetwear, nhiều ngăn.", "429000", "359000", "58", "2"},
                {"Quần suông linen", "Thoáng mát, mùa hè.", "519000", "439000", "44", "3"}
        };
        return mapRows(cat, rows, "PANTS");
    }

    private List<Product> buildShoes(Category cat) {
        String[][] rows = {
                {"Giày sneaker trắng", "Đế cao su, đi hàng ngày.", "899000", "749000", "80", "1"},
                {"Giày chạy bộ lightweight", "Đệm EVA, upper mesh thoáng.", "1199000", "999000", "55", "2"},
                {"Giày da nam công sở", "Derby cổ điển, da thật.", "1599000", "1349000", "40", "3"},
                {"Sandal quai hậu", "Quai da PU, đế PU.", "349000", "299000", "95", "4"},
                {"Dép slide unisex", "Đế dày chống trượt.", "199000", "159000", "150", "5"},
                {"Giày boot cổ thấp", "Da lộn, phối quần jean.", "1399000", "1199000", "36", "1"},
                {"Giày bóng đá sân cỏ nhân tạo", "Đinh TF, upper synthetic.", "799000", "659000", "72", "2"},
                {"Giày loafer da lộn", "Không dây, slip-on tiện.", "999000", "849000", "47", "3"}
        };
        return mapRows(cat, rows, "SHOES");
    }

    private List<Product> buildLaptops(Category cat) {
        String[][] rows = {
                {"MacBook Air M3", "Màn 13.6 Liquid Retina, pin cả ngày.", "28990000", "26990000", "25", "1"},
                {"Dell XPS 15", "OLED 3.5K, RTX laptop GPU.", "52990000", "49990000", "12", "2"},
                {"Lenovo ThinkPad E14", "Bàn phím ThinkPad, bảo mật doanh nghiệp.", "16990000", "15490000", "30", "3"},
                {"ASUS ROG Zephyrus", "Gaming mỏng, tản nhiệt cao cấp.", "45990000", "42990000", "10", "4"},
                {"HP Pavilion 15", "Đủ dùng học tập & văn phòng.", "13990000", "12490000", "38", "5"},
                {"MSI Modern 14", "Nhẹ ~1.4kg, pin ổn định.", "15990000", "14490000", "22", "1"},
                {"Acer Aspire 5", "Giá hợp lý, SSD NVMe.", "11490000", "10490000", "45", "2"},
                {"Surface Laptop 6", "Màn PixelSense cảm ứng.", "35990000", "33490000", "8", "3"},
                {"LG Gram 17", "Siêu nhẹ dưới 1.4kg cho màn 17 inch.", "42990000", "39990000", "7", "4"}
        };
        return mapRows(cat, rows, "LAPTOP");
    }

    private List<Product> buildHats(Category cat) {
        String[][] rows = {
                {"Mũ lưỡi trai cotton", "Chống nắng, nhiều màu.", "159000", "129000", "200", "1"},
                {"Nón bucket chống nắng", "Gấp gọn du lịch.", "189000", "149000", "160", "2"},
                {"Mũ len mùa đông", "Acrylic mềm, giữ ấm.", "129000", "99000", "140", "3"},
                {"Nón snapback streetwear", "Thêu logo phẳng.", "229000", "199000", "90", "4"},
                {"Mũ fedora classic", "Phối vest / áo khoác.", "399000", "329000", "35", "5"},
                {"Mũ bảo hiểm half-face", "Tiêu chuẩn an toàn cơ bản.", "590000", "490000", "55", "1"},
                {"Khăn bandana đa năng", "Cổ / đầu / túi phụ kiện.", "79000", "59000", "300", "2"},
                {"Nón len beanie", "Trơn, unisex.", "99000", "79000", "175", "3"}
        };
        return mapRows(cat, rows, "HAT");
    }

    private List<Product> mapRows(Category cat, String[][] rows, String prefix) {
        List<Product> out = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            String[] r = rows[i];
            int price = Integer.parseInt(r[2]);
            int disc = Integer.parseInt(r[3]);
            int stock = Integer.parseInt(r[4]);
            long brand = Long.parseLong(r[5]);
            double rating = 3.6 + (i % 13) * 0.1;
            boolean feat = (i + rows.length) % 5 == 0;
            String key = prefix.toLowerCase() + "-" + (i + 1);
            out.add(product(cat, r[0], r[1], price, disc, stock, brand, rating, feat, key));
        }
        return out;
    }
}
