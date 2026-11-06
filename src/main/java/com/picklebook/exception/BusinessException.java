package com.picklebook.exception;

/**
 * Lỗi nghiệp vụ có thông điệp hiển thị trực tiếp cho người dùng.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
