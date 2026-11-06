package com.picklebook.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picklebook.dto.request.BookingForm;
import com.picklebook.dto.view.AvailabilitySlot;
import com.picklebook.entity.*;
import com.picklebook.entity.enums.*;
import com.picklebook.exception.ResourceNotFoundException;
import com.picklebook.exception.SlotUnavailableException;
import com.picklebook.repository.*;
import com.picklebook.security.TokenService;
import com.picklebook.service.CourtBookingService;
import com.picklebook.service.EmailService;
import com.picklebook.service.SettingService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class CourtBookingServiceImpl implements CourtBookingService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);

    private final CourtRepository courtRepository;
    private final BookingRepository bookingRepository;
    private final BookingSlotRepository bookingSlotRepository;
    private final CourtScheduleRepository scheduleRepository;
    private final BlockedTimeRepository blockedTimeRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final CouponRepository couponRepository;
    private final ExtraServiceRepository extraServiceRepository;
    private final CustomQuestionRepository questionRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SettingService settingService;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public CourtBookingServiceImpl(CourtRepository courtRepository,
                                   BookingRepository bookingRepository,
                                   BookingSlotRepository bookingSlotRepository,
                                   CourtScheduleRepository scheduleRepository,
                                   BlockedTimeRepository blockedTimeRepository,
                                   PricingRuleRepository pricingRuleRepository,
                                   CouponRepository couponRepository,
                                   ExtraServiceRepository extraServiceRepository,
                                   CustomQuestionRepository questionRepository,
                                   PaymentRepository paymentRepository,
                                   UserRepository userRepository,
                                   EmailService emailService,
                                   SettingService settingService,
                                   TokenService tokenService,
                                   ObjectMapper objectMapper,
                                   @Value("${app.base-url:http://localhost:8081}") String baseUrl) {
        this.courtRepository = courtRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSlotRepository = bookingSlotRepository;
        this.scheduleRepository = scheduleRepository;
        this.blockedTimeRepository = blockedTimeRepository;
        this.pricingRuleRepository = pricingRuleRepository;
        this.couponRepository = couponRepository;
        this.extraServiceRepository = extraServiceRepository;
        this.questionRepository = questionRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.settingService = settingService;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Court> getBookableCourts() {
        return courtRepository.findWithImagesByStatus(CourtStatus.ACTIVE, CourtStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Court> getBookableCourts(long branchId) {
        return courtRepository.findWithImagesByStatusAndBranchId(
                CourtStatus.ACTIVE, CourtStatus.ACTIVE, branchId);
    }

    @Override
    @Transactional(readOnly = true)
    public Court getBookableCourt(String slug) {
        Court court = courtRepository.findWithImagesBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân."));
        if (!court.isBookable()) {
            throw new ResourceNotFoundException("Sân hiện không nhận đặt lịch.");
        }
        return court;
    }

    @Override
    @Transactional(readOnly = true)
    public Court getBookableCourt(long courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân."));
        if (!court.isBookable()) {
            throw new ResourceNotFoundException("Sân hiện không nhận đặt lịch.");
        }
        return court;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomQuestion> getActiveQuestions() {
        List<CustomQuestion> questions = questionRepository.findByActiveTrueOrderBySortOrderAsc();
        for (CustomQuestion question : questions) {
            if (question.getOptionsJson() != null && !question.getOptionsJson().isBlank()) {
                try {
                    question.setOptions(objectMapper.readValue(question.getOptionsJson(), new TypeReference<>() {}));
                } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                    throw new IllegalStateException("Lựa chọn câu hỏi " + question.getId() + " không hợp lệ.", ex);
                }
            }
        }
        return questions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilitySlot> getAvailability(long courtId, LocalDate date) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân."));
        if (!court.isBookable()) {
            return List.of();
        }
        return buildAvailability(court, date, LocalDateTime.now(BUSINESS_ZONE));
    }

    @Override
    @Transactional
    public Booking createBooking(BookingForm form, User authenticatedUser) {
        Court court = courtRepository.findByIdForUpdate(form.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân."));
        if (!court.isBookable()) {
            throw new SlotUnavailableException("Sân hiện không nhận đặt lịch.");
        }
        LocalDate date = Objects.requireNonNull(form.getDate(), "Ngày đặt sân không hợp lệ.");
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        List<LocalTime> requestedStarts = parseAndValidateStarts(form.getSlotStarts());
        if (requestedStarts.isEmpty() || requestedStarts.size() > court.getMaxSlotsPerBooking()) {
            throw new SlotUnavailableException("Số khung giờ chọn không hợp lệ.");
        }
        for (int i = 1; i < requestedStarts.size(); i++) {
            if (!requestedStarts.get(i - 1).plusMinutes(court.getSlotMinutes()).equals(requestedStarts.get(i))) {
                throw new SlotUnavailableException("Các khung giờ phải liên tiếp trong cùng một ngày.");
            }
        }

        Map<LocalDateTime, AvailabilitySlot> availability = new HashMap<>();
        for (AvailabilitySlot slot : buildAvailability(court, date, now)) {
            availability.put(slot.startAt(), slot);
        }
        for (LocalTime start : requestedStarts) {
            AvailabilitySlot slot = availability.get(date.atTime(start));
            if (slot == null || !slot.available()) {
                throw new SlotUnavailableException("Khung giờ vừa hết hoặc không còn khả dụng. Vui lòng chọn lại.");
            }
        }

        long userId = Objects.requireNonNull(authenticatedUser.getId(), "Tài khoản chưa được lưu.");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        Booking booking = new Booking();
        String code = "PB" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        String rawManageToken = tokenService.generateToken();
        booking.setBookingCode(code);
        booking.setManageTokenHash(tokenService.hash(rawManageToken));
        booking.setUser(user);
        booking.setCourt(court);
        booking.setCustomerName(validatedValue(form.getCustomerName(), user.getFullName(), 120));
        booking.setCustomerPhone(validatedValue(form.getCustomerPhone(), user.getPhone(), 20));
        booking.setCustomerEmail(validatedValue(form.getCustomerEmail(), user.getEmail(), 160));
        if (booking.getCustomerName().isBlank() || booking.getCustomerPhone().isBlank()) {
            throw new SlotUnavailableException("Vui lòng bổ sung họ tên và số điện thoại trong hồ sơ.");
        }
        booking.setPlayerCount(form.getPlayerCount());
        booking.setNote(trimToNull(form.getNote()));
        booking.setStatus(BookingStatus.PENDING);

        BigDecimal slotAmount = BigDecimal.ZERO;
        for (LocalTime start : requestedStarts) {
            LocalDateTime startAt = date.atTime(start);
            LocalDateTime endAt = startAt.plusMinutes(court.getSlotMinutes());
            BigDecimal price = priceFor(court, date, startAt.toLocalTime(), endAt.toLocalTime(), now);
            BookingSlot bookingSlot = new BookingSlot();
            bookingSlot.setCourt(court);
            bookingSlot.setStartAt(startAt);
            bookingSlot.setEndAt(endAt);
            bookingSlot.setPrice(price);
            booking.addSlot(bookingSlot);
            slotAmount = slotAmount.add(price);
        }
        booking.setStartAt(date.atTime(requestedStarts.get(0)));
        booking.setEndAt(date.atTime(requestedStarts.get(requestedStarts.size() - 1))
                .plusMinutes(court.getSlotMinutes()));
        booking.setSlotAmount(slotAmount);

        BigDecimal extraAmount = addExtras(form, booking);
        booking.setExtraAmount(extraAmount);
        addAnswers(form, booking);
        Coupon coupon = applyCoupon(form.getCouponCode(), slotAmount.add(extraAmount), now);
        if (coupon != null) {
            booking.setCoupon(coupon);
            coupon.setUsedCount(coupon.getUsedCount() + 1);
        }
        BigDecimal discount = calculateDiscount(coupon, slotAmount.add(extraAmount));
        booking.setDiscountAmount(discount);
        booking.setFinalAmount(slotAmount.add(extraAmount).subtract(discount).max(BigDecimal.ZERO));

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(form.getPaymentMethod());
        } catch (IllegalArgumentException ex) {
            throw new SlotUnavailableException("Phương thức thanh toán không hợp lệ.");
        }
        booking.setPaymentMethod(method);
        if (method == PaymentMethod.ONLINE) {
            booking.setPaymentStatus(PaymentStatus.PAID);
        }
        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new SlotUnavailableException("Khung giờ vừa được người khác đặt. Vui lòng chọn khung giờ khác.");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getFinalAmount());
        payment.setMethod(method);
        payment.setStatus(method == PaymentMethod.ONLINE ? PaymentTxnStatus.SUCCESS : PaymentTxnStatus.PENDING);
        if (method == PaymentMethod.ONLINE) {
            payment.setPaidAt(now);
            payment.setTransactionRef("DEMO-" + booking.getBookingCode());
            payment.setNote("Giao dịch thanh toán mô phỏng.");
        } else if (method == PaymentMethod.BANK_TRANSFER) {
            payment.setNote("Chờ xác nhận chuyển khoản.");
        } else {
            payment.setNote("Thanh toán tại sân.");
        }
        paymentRepository.save(payment);

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", booking.getCustomerName());
        variables.put("customerName", booking.getCustomerName());
        variables.put("court", court.getName());
        variables.put("date", date.toString());
        variables.put("time", booking.getStartAt().toLocalTime() + " - " + booking.getEndAt().toLocalTime());
        variables.put("bookingCode", booking.getBookingCode());
        variables.put("amount", booking.getFinalAmount().toPlainString());
        variables.put("total", booking.getFinalAmount().toPlainString() + " ₫");
        variables.put("paymentMethod", booking.getPaymentMethod().getLabel());
        variables.put("manageUrl", baseUrl + "/quan-ly-dat-san?token=" + rawManageToken);
        emailService.sendTemplate(EmailTemplateCode.BOOKING_CONFIRMATION, booking.getCustomerEmail(), variables);
        return booking;
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingByCode(String code) {
        return bookingRepository.findWithCourtAndUserByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân."));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Booking> getBookingsForUser(Long userId, int page) {
        return bookingRepository.findByUserIdOrderByStartAtDesc(userId, PageRequest.of(Math.max(0, page), 50));
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingForUser(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findWithCourtAndUserById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân."));
        if (booking.getUser() == null || !booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy đơn đặt sân.");
        }
        return booking;
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingForManagementToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResourceNotFoundException("Liên kết quản lý đơn không hợp lệ.");
        }
        return bookingRepository.findWithCourtAndUserByManageTokenHash(tokenService.hash(rawToken))
                .orElseThrow(() -> new ResourceNotFoundException("Liên kết quản lý đơn không hợp lệ."));
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId, Long userId, String reason) {
        Booking booking = bookingRepository.findWithCourtAndUserById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân."));
        if (booking.getUser() == null || !booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy đơn đặt sân.");
        }
        cancelBooking(booking, reason);
    }

    @Override
    @Transactional
    public void cancelBookingByManagementToken(String rawToken, String reason) {
        Booking booking = getBookingForManagementToken(rawToken);
        cancelBooking(booking, reason);
    }

    @Override
    @Transactional
    public void rescheduleBooking(Long bookingId, Long userId, LocalDate date, List<String> slotStarts) {
        Booking booking = bookingRepository.findWithCourtAndUserById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân."));
        if (booking.getUser() == null || !booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy đơn đặt sân.");
        }
        rescheduleBooking(booking, date, slotStarts);
    }

    @Override
    @Transactional
    public void rescheduleBookingByManagementToken(String rawToken, LocalDate date, List<String> slotStarts) {
        rescheduleBooking(getBookingForManagementToken(rawToken), date, slotStarts);
    }

    private void rescheduleBooking(Booking booking, LocalDate date, List<String> slotStarts) {
        int allowedHours = settingService.getInt("booking.reschedule_before_hours", 12);
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        if (!booking.isCancellable() || now.isAfter(booking.getStartAt().minusHours(allowedHours))) {
            throw new SlotUnavailableException("Đơn không còn đủ điều kiện đổi lịch.");
        }
        Court court = courtRepository.findByIdForUpdate(booking.getCourt().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân."));
        List<LocalTime> requestedStarts = parseAndValidateStarts(slotStarts);
        if (date == null || requestedStarts.isEmpty() || requestedStarts.size() > court.getMaxSlotsPerBooking()) {
            throw new SlotUnavailableException("Ngày hoặc số khung giờ đổi lịch không hợp lệ.");
        }
        for (int i = 1; i < requestedStarts.size(); i++) {
            if (!requestedStarts.get(i - 1).plusMinutes(court.getSlotMinutes()).equals(requestedStarts.get(i))) {
                throw new SlotUnavailableException("Các khung giờ phải liên tiếp trong cùng một ngày.");
            }
        }
        Map<LocalDateTime, AvailabilitySlot> availability = new HashMap<>();
        for (AvailabilitySlot slot : buildAvailability(court, date, now, booking.getId())) {
            availability.put(slot.startAt(), slot);
        }
        for (LocalTime start : requestedStarts) {
            AvailabilitySlot slot = availability.get(date.atTime(start));
            if (slot == null || !slot.available()) {
                throw new SlotUnavailableException("Khung giờ mới vừa hết hoặc không khả dụng.");
            }
        }

        bookingSlotRepository.releaseByBookingId(booking.getId());
        BigDecimal slotAmount = BigDecimal.ZERO;
        for (LocalTime start : requestedStarts) {
            LocalDateTime startAt = date.atTime(start);
            LocalDateTime endAt = startAt.plusMinutes(court.getSlotMinutes());
            BigDecimal price = priceFor(court, date, startAt.toLocalTime(), endAt.toLocalTime(), now);
            BookingSlot slot = new BookingSlot();
            slot.setCourt(court);
            slot.setStartAt(startAt);
            slot.setEndAt(endAt);
            slot.setPrice(price);
            booking.addSlot(slot);
            slotAmount = slotAmount.add(price);
        }
        booking.setStartAt(date.atTime(requestedStarts.get(0)));
        booking.setEndAt(date.atTime(requestedStarts.get(requestedStarts.size() - 1))
                .plusMinutes(court.getSlotMinutes()));
        booking.setSlotAmount(slotAmount);
        BigDecimal subtotal = slotAmount.add(booking.getExtraAmount());
        booking.setDiscountAmount(calculateDiscount(booking.getCoupon(), subtotal));
        booking.setFinalAmount(subtotal.subtract(booking.getDiscountAmount()).max(BigDecimal.ZERO));
        booking.setReminderSentAt(null);

        emailService.sendTemplate(EmailTemplateCode.BOOKING_RESCHEDULED, booking.getCustomerEmail(),
                bookingVariables(booking));
    }

    private void cancelBooking(Booking booking, String reason) {
        int allowedHours = settingService.getInt("booking.cancel_before_hours", 12);
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        if (!booking.isCancellable() || now.isAfter(booking.getStartAt().minusHours(allowedHours))) {
            throw new SlotUnavailableException("Đơn không còn đủ điều kiện hủy.");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        booking.setCancelReason(reason == null || reason.isBlank() ? "Khách hàng yêu cầu hủy" : reason.trim());
        bookingSlotRepository.releaseByBookingId(booking.getId());
        emailService.sendTemplate(EmailTemplateCode.BOOKING_CANCELLED, booking.getCustomerEmail(),
                bookingVariables(booking));
    }

    private Map<String, Object> bookingVariables(Booking booking) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", booking.getCustomerName());
        variables.put("customerName", booking.getCustomerName());
        variables.put("court", booking.getCourt().getName());
        variables.put("date", booking.getStartAt().toLocalDate().toString());
        variables.put("time", booking.getStartAt().toLocalTime() + " - " + booking.getEndAt().toLocalTime());
        variables.put("bookingCode", booking.getBookingCode());
        variables.put("amount", booking.getFinalAmount().toPlainString());
        variables.put("total", booking.getFinalAmount().toPlainString() + " ₫");
        variables.put("paymentMethod", booking.getPaymentMethod().getLabel());
        return variables;
    }

    private List<AvailabilitySlot> buildAvailability(Court court, LocalDate date, LocalDateTime now) {
        return buildAvailability(court, date, now, -1L);
    }

    private List<AvailabilitySlot> buildAvailability(Court court, LocalDate date, LocalDateTime now,
                                                     Long excludeBookingId) {
        if (date == null || date.isBefore(now.toLocalDate())
                || date.isAfter(now.toLocalDate().plusDays(court.getMaxAdvanceDays()))) {
            return List.of();
        }
        int dayOfWeek = date.getDayOfWeek().getValue();
        CourtSchedule schedule = scheduleRepository.findByCourtIdAndDayOfWeek(court.getId(), dayOfWeek)
                .orElse(null);
        if (schedule == null || schedule.isClosed()) {
            return List.of();
        }
        List<PricingRule> rules = pricingRuleRepository.findApplicable(court.getId(), court.getBranch().getId());
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<BlockedTime> blocked = blockedTimeRepository.findOverlapping(court.getId(), dayStart, dayEnd);
        List<BlockedTime> holidays = new ArrayList<>();
        for (BlockedTime block : blocked) {
            if (block.isAffectsPricing()) {
                holidays.add(block);
            }
        }
        LocalDateTime cursor = date.atTime(schedule.getOpenTime());
        LocalDateTime closing = date.atTime(schedule.getCloseTime());
        List<AvailabilitySlot> result = new ArrayList<>();
        while (!cursor.plusMinutes(court.getSlotMinutes()).isAfter(closing)) {
            LocalDateTime slotStart = cursor;
            LocalDateTime end = slotStart.plusMinutes(court.getSlotMinutes());
            LocalDateTime conflictStart = slotStart.minusMinutes(court.getBufferMinutes());
            LocalDateTime conflictEnd = end.plusMinutes(court.getBufferMinutes());
            boolean maintenance = blocked.stream().anyMatch(block ->
                    block.getStartAt().isBefore(end) && block.getEndAt().isAfter(slotStart));
            boolean pastNotice = slotStart.isBefore(now.plusMinutes(court.getMinNoticeMinutes()));
            boolean booked = bookingSlotRepository.countActiveOverlap(
                    court.getId(), conflictStart, conflictEnd, excludeBookingId) > 0;
            boolean available = !maintenance && !pastNotice && !booked;
            String status = maintenance ? "MAINTENANCE" : pastNotice ? "PAST" : booked ? "BOOKED" : "AVAILABLE";
            BigDecimal price = priceFor(court, date, slotStart.toLocalTime(), end.toLocalTime(), now, rules, holidays);
            result.add(new AvailabilitySlot(slotStart, end, price, status, available));
            cursor = slotStart.plusMinutes(court.getSlotMinutes());
        }
        return result;
    }

    private BigDecimal priceFor(Court court, LocalDate date, LocalTime start, LocalTime end, LocalDateTime now) {
        LocalDateTime startAt = date.atTime(start);
        LocalDateTime endAt = date.atTime(end);
        Long branchId = (court.getBranch() != null) ? court.getBranch().getId() : null;
        List<PricingRule> rules = pricingRuleRepository.findApplicable(court.getId(), branchId);
        List<BlockedTime> holidays = blockedTimeRepository.findHolidaysOverlapping(court.getId(), startAt, endAt);
        return priceFor(court, date, start, end, now, rules, holidays);
    }

    private BigDecimal priceFor(Court court, LocalDate date, LocalTime start, LocalTime end,
                                LocalDateTime now, List<PricingRule> rules, List<BlockedTime> holidays) {
        DayType dayType = holidays.isEmpty()
                ? (date.getDayOfWeek().getValue() >= 6 ? DayType.WEEKEND : DayType.WEEKDAY)
                : DayType.HOLIDAY;
        BigDecimal rate = court.getBasePrice();
        for (PricingRule rule : rules) {
            if (rule.getDayType() == dayType && rule.matches(start) && !end.isAfter(rule.getEndTime())) {
                rate = rule.getPricePerHour();
                break;
            }
        }
        long minutes = Duration.between(start, end).toMinutes();
        return rate.multiply(BigDecimal.valueOf(minutes)).divide(SIXTY, 0, RoundingMode.HALF_UP);
    }

    private List<LocalTime> parseAndValidateStarts(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new SlotUnavailableException("Vui lòng chọn ít nhất một khung giờ.");
        }
        try {
            return values.stream().map(LocalTime::parse).distinct().sorted().toList();
        } catch (DateTimeParseException ex) {
            throw new SlotUnavailableException("Khung giờ được chọn không hợp lệ.");
        }
    }

    private BigDecimal addExtras(BookingForm form, Booking booking) {
        BigDecimal total = BigDecimal.ZERO;
        Set<Long> selected = new HashSet<>(form.getExtraServiceIds() == null ? List.of() : form.getExtraServiceIds());
        for (ExtraService service : extraServiceRepository.findByActiveTrueOrderBySortOrderAsc()) {
            if (selected.contains(service.getId())) {
                BookingExtraService extra = new BookingExtraService();
                extra.setExtraService(service);
                extra.setQuantity(1);
                extra.setUnitPrice(service.getPrice());
                extra.setAmount(service.getPrice());
                booking.addExtraService(extra);
                total = total.add(service.getPrice());
            }
        }
        return total;
    }

    private void addAnswers(BookingForm form, Booking booking) {
        Map<Long, String> answers = form.getAnswers() == null ? Map.of() : form.getAnswers();
        for (CustomQuestion question : questionRepository.findByActiveTrueOrderBySortOrderAsc()) {
            String answer = trimToNull(answers.get(question.getId()));
            if (question.isRequired() && answer == null) {
                throw new SlotUnavailableException("Vui lòng trả lời: " + question.getLabel());
            }
            if (answer != null) {
                if (answer.length() > 2000) {
                    throw new SlotUnavailableException("Câu trả lời quá dài.");
                }
                if (question.getFieldType() == QuestionType.SELECT || question.getFieldType() == QuestionType.CHECKBOX) {
                    try {
                        List<String> options = objectMapper.readValue(question.getOptionsJson(),
                                new TypeReference<>() {});
                        if (!options.contains(answer)) {
                            throw new SlotUnavailableException("Giá trị không hợp lệ cho câu hỏi: " + question.getLabel());
                        }
                    } catch (SlotUnavailableException ex) {
                        throw ex;
                    } catch (JsonProcessingException ex) {
                        throw new IllegalStateException("Không thể đọc lựa chọn của câu hỏi " + question.getId(), ex);
                    }
                }
                BookingAnswer bookingAnswer = new BookingAnswer();
                bookingAnswer.setQuestion(question);
                bookingAnswer.setAnswer(answer);
                booking.addAnswer(bookingAnswer);
            }
        }
    }

    private Coupon applyCoupon(String code, BigDecimal subtotal, LocalDateTime now) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Coupon coupon = couponRepository.findByCodeForUpdate(code.trim())
                .orElseThrow(() -> new SlotUnavailableException("Mã giảm giá không tồn tại."));
        if (!coupon.isValidAt(now) || subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new SlotUnavailableException("Mã giảm giá hết hạn hoặc chưa đủ điều kiện áp dụng.");
        }
        return coupon;
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        if (coupon == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = coupon.getDiscountType() == DiscountType.PERCENT
                ? subtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : coupon.getDiscountValue();
        if (coupon.getMaxDiscount() != null) {
            discount = discount.min(coupon.getMaxDiscount());
        }
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }

    private String validatedValue(String submitted, String fallback, int maxLength) {
        String value = submitted == null || submitted.isBlank() ? Objects.toString(fallback, "") : submitted.trim();
        if (value.length() > maxLength) {
            throw new SlotUnavailableException("Thông tin khách hàng vượt quá độ dài cho phép.");
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
