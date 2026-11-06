package com.picklebook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Điểm khởi động của ứng dụng PickleBook.
 *
 * <p>Ứng dụng chạy mặc định với profile {@code dev} (H2 in-memory) để có thể
 * khởi động ngay mà không cần cài MySQL. Khi triển khai thật, chạy với
 * {@code --spring.profiles.active=mysql}.</p>
 */
@EnableScheduling
@SpringBootApplication
public class PickleBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(PickleBookApplication.class, args);
    }
}
