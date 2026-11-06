package com.picklebook.service;

import com.picklebook.dto.request.RecurringContractForm;
import com.picklebook.dto.view.RecurringSchedulePreviewResult;
import com.picklebook.entity.RecurringContract;
import com.picklebook.entity.User;

import java.time.LocalTime;
import java.util.List;

public interface RecurringBookingService {

    /**
     * Xem trước lịch đặt cố định trong tháng và kiểm tra trùng lịch từng buổi
     */
    RecurringSchedulePreviewResult previewSchedule(Long courtId, int year, int month,
                                                   List<String> dayOfWeekNames,
                                                   LocalTime startTime, LocalTime endTime);

    /**
     * Tạo yêu cầu hợp đồng đặt sân cố định theo tháng
     */
    RecurringContract createContract(RecurringContractForm form, User user);

    /**
     * Lấy chi tiết hợp đồng theo mã code
     */
    RecurringContract getContractByCode(String contractCode);

    /**
     * Lấy chi tiết hợp đồng theo ID
     */
    RecurringContract getContractById(Long id);

    /**
     * Phê duyệt hợp đồng cố định và giữ slot cho các buổi trong tháng
     */
    void approveContract(Long contractId);

    /**
     * Hủy hợp đồng cố định
     */
    void cancelContract(Long contractId, String reason);

    /**
     * Danh sách tất cả hợp đồng cố định (Admin)
     */
    List<RecurringContract> getAllContracts();

    /**
     * Danh sách hợp đồng của người dùng
     */
    List<RecurringContract> getUserContracts(Long userId);
}

