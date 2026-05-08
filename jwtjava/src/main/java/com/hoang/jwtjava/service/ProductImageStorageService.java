package com.hoang.jwtjava.service;

import com.hoang.jwtjava.config.StorageProperties;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageStorageService {

    private static final String USER_AGENT = "JwtjavaProductBot/1.0";

    private static final Map<String, String> MIME_TO_EXT = Map.of(
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private static final Map<String, String> EXT_FROM_FILENAME = Map.of(
            ".jpg", ".jpg",
            ".jpeg", ".jpg",
            ".png", ".png",
            ".gif", ".gif",
            ".webp", ".webp"
    );

    private final StorageProperties props;
    private final ObjectProvider<CloudinaryUploadService> cloudinaryUploadService;

    private Path imagesDir;
    private HttpClient httpClient;

    @PostConstruct
    void init() {
        try {
            Path root = Path.of(props.getRoot()).toAbsolutePath().normalize();
            imagesDir = root.resolve("product-images");
            Files.createDirectories(imagesDir);
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(Math.min(props.getDownloadTimeoutSeconds(), 60)))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Không khởi tạo được thư mục lưu ảnh", e);
        }
    }

    /**
     * Với mỗi chuỗi:
     * - Khi Cloudinary bật: giữ nguyên URL http(s) để tránh kéo ảnh cloud về local storage.
     * - Khi Cloudinary tắt: URL http(s) sẽ được tải về local storage như trước.
     * - Các giá trị không phải URL http(s) được giữ nguyên (path DB hoặc dữ liệu cũ).
     */
    public List<String> resolveImageUrlsForPersistence(List<String> inputs) {
        if (inputs == null || inputs.isEmpty())
            return List.of();
        CloudinaryUploadService cloudinary = cloudinaryUploadService.getIfAvailable();
        boolean cloudinaryEnabled = cloudinary != null;
        List<String> out = new ArrayList<>(inputs.size());
        for (String raw : inputs) {
            if (raw == null || raw.isBlank())
                continue;
            String s = raw.trim();
            if (isHttpUrl(s)) {
                if (cloudinaryEnabled)
                    out.add(s);
                else
                    out.add(downloadAndStore(s));
            } else
                out.add(s);
        }
        return out;
    }

    /**
     * Import danh sách URL http(s) vào storage quản lý:
     * - Cloudinary bật: upload remote URL lên Cloudinary và trả secure_url.
     * - Cloudinary tắt: tải về local storage như luồng cũ.
     * Các giá trị không phải http(s) được giữ nguyên.
     */
    public List<String> importHttpImageUrlsToManagedStorage(List<String> inputs) {
        if (inputs == null || inputs.isEmpty())
            return List.of();
        CloudinaryUploadService cloudinary = cloudinaryUploadService.getIfAvailable();
        List<String> out = new ArrayList<>(inputs.size());
        for (String raw : inputs) {
            if (raw == null || raw.isBlank())
                continue;
            String s = raw.trim();
            if (!isHttpUrl(s)) {
                out.add(s);
                continue;
            }
            if (cloudinary != null)
                out.add(cloudinary.uploadImageFromUrl(s));
            else
                out.add(downloadAndStore(s));
        }
        return out;
    }

    /**
     * Lưu các file upload (multipart), trả về URL/path giống luồng tải từ HTTP — dùng cho trường {@code images} khi tạo/cập nhật sản phẩm.
     */
    public List<String> saveUploadedFiles(List<MultipartFile> files) {
        CloudinaryUploadService cloudinary = cloudinaryUploadService.getIfAvailable();
        if (cloudinary != null)
            return cloudinary.uploadProductImages(files);

        if (files == null || files.isEmpty())
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);
        List<String> out = new ArrayList<>();
        boolean any = false;
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty())
                continue;
            any = true;
            out.add(saveMultipart(f));
        }
        if (!any)
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);
        return out;
    }

    private String saveMultipart(MultipartFile file) {
        if (file.getSize() > props.getMaxBytes())
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);

        String contentType = "";
        if (StringUtils.hasText(file.getContentType()))
            contentType = file.getContentType().split(";")[0].trim().toLowerCase(Locale.ROOT);

        String ext = MIME_TO_EXT.get(contentType);
        if (ext == null)
            ext = extFromOriginalFilename(file.getOriginalFilename());
        if (ext == null)
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);

        String fileName = UUID.randomUUID() + ext;
        Path target = imagesDir.resolve(fileName);

        try (InputStream in = file.getInputStream()) {
            copyStreamBounded(in, target, props.getMaxBytes());
        } catch (IOException e) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
            }
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);
        }

        return buildStoredUrl(fileName);
    }

    private String extFromOriginalFilename(String original) {
        if (!StringUtils.hasText(original))
            return null;
        String lower = original.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        if (dot < 0 || dot == lower.length() - 1)
            return null;
        return EXT_FROM_FILENAME.get(lower.substring(dot));
    }

    private boolean isHttpUrl(String s) {
        try {
            URI u = URI.create(s);
            String scheme = u.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (Exception e) {
            return false;
        }
    }

    private String downloadAndStore(String urlString) {
        URI uri;
        try {
            uri = URI.create(urlString).normalize();
        } catch (Exception e) {
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(props.getDownloadTimeoutSeconds()))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int code = response.statusCode();
            if (code < 200 || code >= 300)
                throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);

            long len = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            if (len > props.getMaxBytes())
                throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);

            String contentType = response.headers().firstValue("Content-Type").orElse("").split(";")[0].trim().toLowerCase(Locale.ROOT);
            if (!MIME_TO_EXT.containsKey(contentType))
                throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);

            String ext = MIME_TO_EXT.get(contentType);
            String fileName = UUID.randomUUID() + ext;
            Path target = imagesDir.resolve(fileName);

            try (InputStream in = response.body()) {
                copyStreamBounded(in, target, props.getMaxBytes());
            } catch (IOException e) {
                Files.deleteIfExists(target);
                throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);
            }

            return buildStoredUrl(fileName);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.IMAGE_IMPORT_FAILED);
        }
    }

    private String buildStoredUrl(String fileName) {
        String basePath = props.getPublicUrlPath().replaceAll("/+$", "");
        if (!basePath.startsWith("/"))
            basePath = "/" + basePath;
        String path = basePath + "/" + fileName;

        String pub = props.getPublicBaseUrl();
        if (StringUtils.hasText(pub)) {
            String b = pub.replaceAll("/+$", "");
            return b + path;
        }
        return path;
    }

    private static void copyStreamBounded(InputStream in, Path target, long maxBytes) throws IOException {
        long total = 0;
        byte[] buf = new byte[8192];
        try (OutputStream out = Files.newOutputStream(target)) {
            int n;
            while ((n = in.read(buf)) >= 0) {
                total += n;
                if (total > maxBytes)
                    throw new IOException("File too large");
                out.write(buf, 0, n);
            }
        }
    }
}
