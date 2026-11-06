package com.picklebook.service.impl;

import com.picklebook.exception.BusinessException;
import com.picklebook.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Lưu ảnh tải lên trên đĩa cục bộ.
 *
 * <p>Bảo mật: tên tệp do hệ thống sinh (UUID) nên không thể path traversal;
 * phần mở rộng suy ra từ MIME type; nội dung được kiểm tra magic bytes để
 * tránh tải lên tệp giả mạo ảnh.</p>
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final Path rootDirectory;

    public FileStorageServiceImpl(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.rootDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn tệp cần tải lên");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException("Tệp tải lên không được vượt quá 5MB");
        }
        String extension = resolveExtension(file.getContentType());
        verifyMagicBytes(file, extension);

        String safeFolder = sanitizeFolder(folder);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            Path targetDirectory = rootDirectory.resolve(safeFolder).normalize();
            if (!targetDirectory.startsWith(rootDirectory)) {
                throw new BusinessException("Thư mục lưu tệp không hợp lệ");
            }
            Files.createDirectories(targetDirectory);
            Path target = targetDirectory.resolve(fileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Đã lưu tệp {} vào {}", fileName, targetDirectory);
            return "/uploads/" + safeFolder + "/" + fileName;
        } catch (IOException ex) {
            log.error("Không lưu được tệp tải lên: {}", ex.getMessage());
            throw new BusinessException("Không lưu được tệp tải lên, vui lòng thử lại");
        }
    }

    @Override
    public void delete(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith("/uploads/")) {
            return;
        }
        try {
            Path file = rootDirectory.resolve(publicUrl.substring("/uploads/".length())).normalize();
            if (file.startsWith(rootDirectory)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ex) {
            log.warn("Không xóa được tệp {}: {}", publicUrl, ex.getMessage());
        }
    }

    private String resolveExtension(String contentType) {
        if (contentType == null) {
            throw new BusinessException("Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP");
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new BusinessException("Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP");
        };
    }

    /**
     * Kiểm tra chữ ký tệp (magic bytes) khớp với phần mở rộng mong đợi.
     */
    private void verifyMagicBytes(MultipartFile file, String extension) {
        byte[] header = new byte[12];
        int read;
        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.read(header);
        } catch (IOException ex) {
            throw new BusinessException("Không đọc được tệp tải lên");
        }
        if (read < 12 || !matchesSignature(header, extension)) {
            throw new BusinessException("Nội dung tệp không phải ảnh hợp lệ");
        }
    }

    private boolean matchesSignature(byte[] header, String extension) {
        return switch (extension) {
            case "jpg" -> (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            case "png" -> (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G';
            case "webp" -> header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }

    private String sanitizeFolder(String folder) {
        if (folder == null || !folder.matches("[a-z0-9_-]{1,40}")) {
            throw new BusinessException("Thư mục lưu tệp không hợp lệ");
        }
        return folder;
    }
}
