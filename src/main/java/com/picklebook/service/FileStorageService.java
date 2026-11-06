package com.picklebook.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Lưu trữ tệp tải lên (ảnh đại diện, ảnh sân, chứng từ chuyển khoản).
 * Triển khai mặc định lưu trên đĩa tại thư mục cấu hình {@code app.upload-dir}.
 */
public interface FileStorageService {

    /**
     * Lưu tệp vào thư mục con và trả về URL công khai (ví dụ {@code /uploads/avatars/abc.png}).
     *
     * @param file   tệp tải lên
     * @param folder thư mục con (chỉ chữ thường, số, gạch ngang)
     * @return đường dẫn URL dùng trực tiếp trong template
     */
    String store(MultipartFile file, String folder);

    /**
     * Xóa tệp theo URL công khai. Bỏ qua nếu URL không thuộc thư mục upload.
     */
    void delete(String publicUrl);
}
