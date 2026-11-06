package com.picklebook.service.impl;

import com.picklebook.dto.request.RecurringContractForm;
import com.picklebook.dto.view.RecurringSchedulePreviewResult;
import com.picklebook.dto.view.RecurringSessionPreviewDto;
import com.picklebook.entity.Court;
import com.picklebook.entity.PricingRule;
import com.picklebook.entity.RecurringContract;
import com.picklebook.entity.RecurringContractSession;
import com.picklebook.entity.User;
import com.picklebook.entity.enums.DayType;
import com.picklebook.entity.enums.PaymentMethod;
import com.picklebook.entity.enums.PaymentStatus;
import com.picklebook.entity.enums.RecurringContractStatus;
import com.picklebook.entity.enums.RecurringSessionStatus;
import com.picklebook.exception.ResourceNotFoundException;
import com.picklebook.exception.SlotUnavailableException;
import com.picklebook.repository.BlockedTimeRepository;
import com.picklebook.repository.BookingSlotRepository;
import com.picklebook.repository.CourtRepository;
import com.picklebook.repository.PricingRuleRepository;
import com.picklebook.repository.RecurringContractRepository;
import com.picklebook.repository.RecurringContractSessionRepository;
import com.picklebook.security.TokenService;
import com.picklebook.service.RecurringBookingService;
import com.picklebook.service.SettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class RecurringBookingServiceImpl implements RecurringBookingService {

    private final RecurringContractRepository contractRepository;
    private final RecurringContractSessionRepository sessionRepository;
    private final CourtRepository courtRepository;
    private final BookingSlotRepository bookingSlotRepository;
    private final BlockedTimeRepository blockedTimeRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final TokenService tokenService;
    private final SettingService settingService;

    public RecurringBookingServiceImpl(RecurringContractRepository contractRepository,
                                       RecurringContractSessionRepository sessionRepository,
                                       CourtRepository courtRepository,
                                       BookingSlotRepository bookingSlotRepository,
                                       BlockedTimeRepository blockedTimeRepository,
                                       PricingRuleRepository pricingRuleRepository,
                                       TokenService tokenService,
                                       SettingService settingService) {
        this.contractRepository = contractRepository;
        this.sessionRepository = sessionRepository;
        this.courtRepository = courtRepository;
        this.bookingSlotRepository = bookingSlotRepository;
        this.blockedTimeRepository = blockedTimeRepository;
        this.pricingRuleRepository = pricingRuleRepository;
        this.tokenService = tokenService;
        this.settingService = settingService;
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringSchedulePreviewResult previewSchedule(Long courtId, int year, int month,
                                                          List<String> dayOfWeekNames,
                                                          LocalTime startTime, LocalTime endTime) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân."));

        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new SlotUnavailableException("Khung giờ bắt đầu và kết thúc không hợp lệ.");
        }

        YearMonth ym = YearMonth.of(year, month);
        LocalDate firstDay = ym.atDay(1);
        LocalDate lastDay = ym.atEndOfMonth();

        Set<DayOfWeek> targetDays = new HashSet<>();
        if (dayOfWeekNames != null) {
            for (String dayName : dayOfWeekNames) {
                try {
                    targetDays.add(DayOfWeek.valueOf(dayName.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (targetDays.isEmpty()) {
            throw new SlotUnavailableException("Vui lòng chọn ít nhất một thứ trong tuần.");
        }

        Long branchId = (court.getBranch() != null) ? court.getBranch().getId() : null;
        List<PricingRule> rules = pricingRuleRepository.findApplicable(court.getId(), branchId);

        List<RecurringSessionPreviewDto> sessionDtos = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int availableCount = 0;

        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            if (!targetDays.contains(date.getDayOfWeek())) {
                continue;
            }

            LocalDateTime startAt = date.atTime(startTime);
            LocalDateTime endAt = date.atTime(endTime);

            // Kiểm tra trùng lịch đặt đơn lẻ
            boolean slotConflict = !bookingSlotRepository.findActiveInRange(courtId, startAt, endAt).isEmpty();

            // Kiểm tra trùng lịch bảo trì/đóng cửa
            boolean maintenanceConflict = !blockedTimeRepository.findOverlapping(courtId, startAt, endAt).isEmpty();

            // Kiểm tra trùng với buổi cố định khác đã được lên lịch
            boolean recurringConflict = !sessionRepository.findConflictingSessions(courtId, date, startTime, endTime).isEmpty();

            boolean available = !slotConflict && !maintenanceConflict && !recurringConflict;
            String conflictReason = null;
            if (slotConflict) {
                conflictReason = "Đã có khách đặt lẻ";
            } else if (maintenanceConflict) {
                conflictReason = "Lịch bảo trì / Sân đóng cửa";
            } else if (recurringConflict) {
                conflictReason = "Đã có hợp đồng cố định khác";
            }

            // Tính giá buổi chơi
            BigDecimal sessionPrice = calculateSessionPrice(court, date, startTime, endTime, rules);
            if (available) {
                totalAmount = totalAmount.add(sessionPrice);
                availableCount++;
            }

            RecurringSessionPreviewDto dto = new RecurringSessionPreviewDto();
            dto.setDate(date);
            dto.setDayOfWeek(date.getDayOfWeek());
            dto.setDayOfWeekLabel(formatDayOfWeekLabel(date.getDayOfWeek()) + ", " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dto.setStartTime(startTime);
            dto.setEndTime(endTime);
            dto.setPrice(sessionPrice);
            dto.setAvailable(available);
            dto.setConflictReason(conflictReason);

            sessionDtos.add(dto);
        }

        // Chính sách giảm giá theo số buổi đặt tháng
        BigDecimal discount = BigDecimal.ZERO;
        if (availableCount >= 8) {
            // Giảm 10% khi đặt từ 8 buổi trở lên (2-3 buổi/tuần suốt tháng)
            discount = totalAmount.multiply(new BigDecimal("0.10")).setScale(0, RoundingMode.HALF_UP);
        } else if (availableCount >= 4) {
            // Giảm 5% khi đặt từ 4 buổi trở lên
            discount = totalAmount.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.HALF_UP);
        }

        BigDecimal finalAmount = totalAmount.subtract(discount).max(BigDecimal.ZERO);
        int depositPercent = settingService.getInt("payment.deposit_percent", 30);
        BigDecimal depositAmount = finalAmount.multiply(BigDecimal.valueOf(depositPercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        BigDecimal pricePerSession = availableCount > 0
                ? totalAmount.divide(BigDecimal.valueOf(availableCount), 0, RoundingMode.HALF_UP)
                : court.getBasePrice();

        RecurringSchedulePreviewResult result = new RecurringSchedulePreviewResult();
        result.setCourtId(court.getId());
        result.setCourtName(court.getName());
        result.setYear(year);
        result.setMonth(month);
        result.setMonthLabel(String.format("Tháng %02d/%d", month, year));
        result.setTotalSessions(sessionDtos.size());
        result.setAvailableSessions(availableCount);
        result.setHasConflicts(availableCount < sessionDtos.size());
        result.setPricePerSession(pricePerSession);
        result.setTotalAmount(totalAmount);
        result.setDiscountAmount(discount);
        result.setFinalAmount(finalAmount);
        result.setDepositAmount(depositAmount);
        result.setSessions(sessionDtos);

        return result;
    }

    @Override
    @Transactional
    public RecurringContract createContract(RecurringContractForm form, User user) {
        LocalTime startTime = LocalTime.parse(form.getStartTime());
        LocalTime endTime = LocalTime.parse(form.getEndTime());

        RecurringSchedulePreviewResult preview = previewSchedule(
                form.getCourtId(), form.getYear(), form.getMonth(),
                form.getDaysOfWeek(), startTime, endTime);

        if (preview.getAvailableSessions() == 0) {
            throw new SlotUnavailableException("Không có buổi chơi nào khả dụng trong tháng được chọn.");
        }

        Court court = courtRepository.findById(form.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân."));

        YearMonth ym = YearMonth.of(form.getYear(), form.getMonth());
        String code = "CD" + form.getYear() + String.format("%02d", form.getMonth())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        String rawToken = tokenService.generateToken();

        RecurringContract contract = new RecurringContract();
        contract.setContractCode(code);
        contract.setCourt(court);
        contract.setUser(user);
        contract.setCustomerName(form.getCustomerName().trim());
        contract.setCustomerPhone(form.getCustomerPhone().trim());
        contract.setCustomerEmail(form.getCustomerEmail().trim());
        contract.setStartDate(ym.atDay(1));
        contract.setEndDate(ym.atEndOfMonth());
        contract.setDaysOfWeek(String.join(",", form.getDaysOfWeek()));
        contract.setStartTime(startTime);
        contract.setEndTime(endTime);
        contract.setTotalSessions(preview.getAvailableSessions());
        contract.setPricePerSession(preview.getPricePerSession());
        contract.setTotalAmount(preview.getTotalAmount());
        contract.setDiscountAmount(preview.getDiscountAmount());
        contract.setDepositAmount(preview.getDepositAmount());
        contract.setFinalAmount(preview.getFinalAmount());

        try {
            contract.setPaymentMethod(PaymentMethod.valueOf(form.getPaymentMethod()));
        } catch (Exception ex) {
            contract.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        }
        contract.setPaymentStatus(PaymentStatus.UNPAID);
        contract.setStatus(RecurringContractStatus.PENDING);
        contract.setNote(form.getNote());
        contract.setManageTokenHash(tokenService.hash(rawToken));

        for (RecurringSessionPreviewDto sessionDto : preview.getSessions()) {
            if (sessionDto.isAvailable()) {
                RecurringContractSession session = new RecurringContractSession();
                session.setSessionDate(sessionDto.getDate());
                session.setStartTime(sessionDto.getStartTime());
                session.setEndTime(sessionDto.getEndTime());
                session.setPrice(sessionDto.getPrice());
                session.setStatus(RecurringSessionStatus.SCHEDULED);
                contract.addSession(session);
            }
        }

        return contractRepository.save(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringContract getContractByCode(String contractCode) {
        return contractRepository.findWithDetailsByContractCode(contractCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng đặt sân cố định: " + contractCode));
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringContract getContractById(Long id) {
        return contractRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng ID: " + id));
    }

    @Override
    @Transactional
    public void approveContract(Long contractId) {
        RecurringContract contract = getContractById(contractId);
        contract.setStatus(RecurringContractStatus.APPROVED);
        contract.setPaymentStatus(PaymentStatus.PAID);
        contractRepository.save(contract);
    }

    @Override
    @Transactional
    public void cancelContract(Long contractId, String reason) {
        RecurringContract contract = getContractById(contractId);
        contract.setStatus(RecurringContractStatus.CANCELLED);
        contract.setNote((contract.getNote() != null ? contract.getNote() + "\n" : "") + "Lý do hủy: " + reason);
        for (RecurringContractSession s : contract.getSessions()) {
            s.setStatus(RecurringSessionStatus.CANCELLED);
        }
        contractRepository.save(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringContract> getAllContracts() {
        return contractRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringContract> getUserContracts(Long userId) {
        return contractRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private BigDecimal calculateSessionPrice(Court court, LocalDate date, LocalTime start, LocalTime end, List<PricingRule> rules) {
        DayType dayType = (date.getDayOfWeek().getValue() >= 6) ? DayType.WEEKEND : DayType.WEEKDAY;
        BigDecimal rate = court.getBasePrice();
        for (PricingRule rule : rules) {
            if (rule.getDayType() == dayType && rule.matches(start) && !end.isAfter(rule.getEndTime())) {
                rate = rule.getPricePerHour();
                break;
            }
        }
        long minutes = Duration.between(start, end).toMinutes();
        return rate.multiply(BigDecimal.valueOf(minutes)).divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP);
    }

    private String formatDayOfWeekLabel(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ Nhật";
        };
    }
}

