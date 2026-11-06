package com.picklebook.exception;

/**
 * Không tìm thấy tài nguyên (sân, đơn, người dùng...).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super("Không tìm thấy " + resource + " với id = " + id);
    }
}
