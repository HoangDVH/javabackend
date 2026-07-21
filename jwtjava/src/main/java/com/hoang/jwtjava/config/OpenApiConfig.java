package com.hoang.jwtjava.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearer-jwt";

    private static final Map<String, Integer> TAG_ORDER = Map.of(
            "Authentication", 0,
            "Categories", 1,
            "Products", 2,
            "Orders", 3,
            "Payments", 4,
            "Users", 5
    );

    @Bean
    public OpenApiCustomizer tagOrderCustomizer() {
        return openApi -> {
            if (openApi.getTags() == null || openApi.getTags().isEmpty())
                return;
            List<Tag> tags = new ArrayList<>(openApi.getTags());
            tags.sort(Comparator.comparingInt(t -> TAG_ORDER.getOrDefault(t.getName(), 99)));
            openApi.setTags(tags);
        };
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JwtJava API")
                        .description("""
                                REST API cho Easy Mart: JWT auth, danh mục, sản phẩm, đơn hàng, thanh toán.
                                
                                **Public (không cần JWT):** GET `/api/v1/products/**`, GET `/api/v1/categories/**`, POST auth register/login/google/introspect/refresh/forgot-password/reset-password.
                                
                                **Redis:** JWT blacklist, rate limit (auth + tạo đơn/thanh toán), cache catalog (TTL 2–30 phút, chống stampede). Khi Redis tắt/lỗi API vẫn trả dữ liệu từ DB.
                                
                                **Rate limit & FE retry:** POST auth / orders / payments có thể trả **429** với header `Retry-After` (giây) và `X-RateLimit-Remaining`. FE nên: (1) đọc `Retry-After`, (2) exponential backoff + jitter (vd 1s → 2s → 4s, max ~30s), (3) không spam lại cùng request khi đang chờ, (4) GET catalog không bị rate limit. Cold start Render Free có thể chậm vài giây — retry nhẹ cho network/5xx, không retry ngay 4xx (trừ 429).
                                
                                **Google Sign-In:** FE lấy Google ID token (GIS), gọi POST `/api/v1/auth/google` `{ "idToken": "..." }`. BE verify + find/create user, trả JWT giống login. Env: `GOOGLE_AUTH_ENABLED`, `GOOGLE_CLIENT_ID`.
                                
                                **Quên mật khẩu:** Resend HTTPS API gửi link reset tới SPA (`APP_FRONTEND_RESET_URL`). Token one-time TTL 30 phút (Redis hoặc DB fallback).
                                
                                **Refresh token:** HttpOnly cookie `refresh_token` trên path `/api/v1/auth` (SameSite=None trên production Render).
                                
                                **Đơn hàng:** POST `/api/v1/orders` nhận `items` + `receiverName` + `receiverPhone` + `shippingAddress`. BE tính `shippingFee` (30k nếu subtotal &lt; 500k) và `totalAmount = subtotal + shippingFee` (COD/VNPay dùng total này). Trừ kho atomic (`stock >= qty`).
                                
                                **Profile & sổ địa chỉ:** GET/PUT `/api/v1/users/me` (fullName, phone, avatarUrl, password). Google login lưu `avatarUrl` từ Google. CRUD `/api/v1/users/me/addresses` với `isDefault`.
                                
                                **Thanh toán:** COD `POST /api/v1/payments` idempotent (đã SUCCESS thì trả payment cũ). VNPay: POST `/api/v1/payments/vnpay` → `paymentUrl`; IPN public cập nhật PAID. Hủy đơn chưa thanh toán: POST `/api/v1/orders/{id}/cancel`.
                                """)
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the access token from POST /api/v1/auth/login (Swagger adds the Bearer prefix).")));
    }
}
