package com.picklebook.exception;

/**
 * Khung giờ đã bị người khác đặt hoặc bị chặn trong lúc khách đang thao tác.
 */
public class SlotUnavailableException extends BusinessException {

    public SlotUnavailableException(String message) {
        super(message);
    }
}
