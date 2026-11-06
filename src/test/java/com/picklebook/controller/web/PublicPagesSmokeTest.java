package com.picklebook.controller.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicPagesSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.picklebook.service.CourtBookingService courtBookingService;

    @Autowired
    private com.picklebook.service.UserService userService;

    @Autowired
    private org.thymeleaf.spring6.SpringTemplateEngine templateEngine;

    @Autowired
    private com.picklebook.service.VietQrService vietQrService;

    @Test
    void publicPagesRenderWithSeedData() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/index"));
        mockMvc.perform(get("/san"))
                .andExpect(status().isOk())
                .andExpect(view().name("courts/list"));
        mockMvc.perform(get("/san").param("branchId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("courts/list"));
        mockMvc.perform(get("/san/san-trung-tam"))
                .andExpect(status().isOk())
                .andExpect(view().name("courts/detail"));
        mockMvc.perform(get("/san/san-goc-pho"))
                .andExpect(status().isOk())
                .andExpect(view().name("courts/detail"));
        mockMvc.perform(get("/san/san-sunrise"))
                .andExpect(status().isOk())
                .andExpect(view().name("courts/detail"));
        mockMvc.perform(get("/san/san-doi-thong"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/courts/1/slots")
                        .param("date", LocalDate.now().plusDays(2).toString()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/gioi-thieu"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/about"));
        mockMvc.perform(get("/chinh-sach"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/policy"));
    }

    @Test
    @WithMockUser(username = "user@picklebook.vn")
    void bookingFormRouteDoesNotReturnServerError() throws Exception {
        mockMvc.perform(get("/dat-san")
                        .param("courtId", "1")
                        .param("date", LocalDate.now().plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/confirm"));
    }

    @Test
    @WithMockUser(username = "user@picklebook.vn")
    void bookingSuccessPageRendersCleanly() throws Exception {
        com.picklebook.dto.request.BookingForm form = new com.picklebook.dto.request.BookingForm();
        form.setCourtId(1L);
        form.setDate(LocalDate.now().plusDays(3));
        form.setSlotStarts(java.util.List.of("07:00"));
        form.setPaymentMethod("BANK_TRANSFER");
        form.setCustomerName("Nguyen Minh Khang");
        form.setCustomerPhone("0909123458");
        form.setCustomerEmail("user@picklebook.vn");
        form.setPlayerCount(2);

        java.util.Map<Long, String> answers = new java.util.HashMap<>();
        for (com.picklebook.entity.CustomQuestion q : courtBookingService.getActiveQuestions()) {
            if (q.isRequired()) {
                String val = (q.getOptions() != null && !q.getOptions().isEmpty())
                        ? q.getOptions().get(0)
                        : "Không cần";
                answers.put(q.getId(), val);
            }
        }
        form.setAnswers(answers);

        com.picklebook.entity.User user = userService.getById(3L);
        com.picklebook.entity.Booking booking = courtBookingService.createBooking(form, user);

        mockMvc.perform(get("/dat-san/thanh-cong/" + booking.getBookingCode()))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/success"));

        // Render template explicitly to verify Thymeleaf template parsing with VietQR
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
        org.thymeleaf.web.servlet.JakartaServletWebApplication app = org.thymeleaf.web.servlet.JakartaServletWebApplication.buildApplication(request.getServletContext());
        org.thymeleaf.web.IWebExchange exchange = app.buildExchange(request, response);
        org.thymeleaf.context.WebContext ctx = new org.thymeleaf.context.WebContext(exchange);
        ctx.setVariable("booking", booking);
        ctx.setVariable("vietQr", vietQrService.generatePaymentInfo(booking.getBookingCode(), booking.getFinalAmount()));
        String html = templateEngine.process("booking/success", ctx);
        org.junit.jupiter.api.Assertions.assertNotNull(html);
        org.junit.jupiter.api.Assertions.assertTrue(html.contains(booking.getBookingCode()));

        // Also test template rendering when vietQr is null (fallback handling)
        org.thymeleaf.context.WebContext ctxNullQr = new org.thymeleaf.context.WebContext(exchange);
        ctxNullQr.setVariable("booking", booking);
        ctxNullQr.setVariable("vietQr", null);
        String htmlNullQr = templateEngine.process("booking/success", ctxNullQr);
        org.junit.jupiter.api.Assertions.assertNotNull(htmlNullQr);
        org.junit.jupiter.api.Assertions.assertTrue(htmlNullQr.contains(booking.getBookingCode()));
    }

    @Test
    void recurringBookingPagesAndPreviewWork() throws Exception {
        mockMvc.perform(get("/dat-co-dinh"))
                .andExpect(status().isOk())
                .andExpect(view().name("booking/recurring"));

        mockMvc.perform(get("/api/recurring/preview")
                        .param("courtId", "1")
                        .param("year", "2026")
                        .param("month", "11")
                        .param("daysOfWeek", "MONDAY", "WEDNESDAY")
                        .param("startTime", "18:00")
                        .param("endTime", "20:00"))
                .andExpect(status().isOk());
    }
}
