package com.picklebook.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Xử lý lỗi tập trung cho toàn bộ controller MVC.
 *
 * <p>Mọi lỗi đều được ghi log ở server, người dùng chỉ thấy thông điệp thân
 * thiện (không lộ stack trace hay chi tiết hệ thống).</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException ex) {
        log.warn("Không tìm thấy dữ liệu: {}", ex.getMessage());
        return build("error/404", HttpStatus.NOT_FOUND,
                ex.getMessage() == null ? "Trang hoặc dữ liệu bạn tìm không tồn tại." : ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleStaticResourceNotFound(NoResourceFoundException ex) {
        log.warn("Không tìm thấy tài nguyên tĩnh: {}", ex.getResourcePath());
        return build("error/404", HttpStatus.NOT_FOUND,
                "Tài nguyên bạn yêu cầu không tồn tại.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(AccessDeniedException ex) {
        log.warn("Từ chối truy cập: {}", ex.getMessage());
        return build("error/403", HttpStatus.FORBIDDEN,
                "Bạn không có quyền truy cập chức năng này.");
    }

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusiness(BusinessException ex) {
        log.warn("Lỗi nghiệp vụ: {}", ex.getMessage());
        return build("error/error", HttpStatus.BAD_REQUEST,
                ex.getMessage() == null ? "Yêu cầu không hợp lệ." : ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        log.warn("Tệp tải lên vượt quá giới hạn: {}", ex.getMessage());
        return build("error/error", HttpStatus.BAD_REQUEST,
                "Tệp tải lên quá lớn. Vui lòng chọn ảnh nhỏ hơn 5MB.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception ex) {
        log.error("Lỗi hệ thống không mong đợi", ex);
        return build("error/500", HttpStatus.INTERNAL_SERVER_ERROR,
                "Hệ thống đang gặp sự cố. Vui lòng thử lại sau ít phút.");
    }

    private ModelAndView build(String viewName, HttpStatus status, String message) {
        ModelAndView modelAndView = new ModelAndView(viewName);
        modelAndView.setStatus(status);
        modelAndView.addObject("message", message);
        modelAndView.addObject("status", status.value());
        return modelAndView;
    }
}
