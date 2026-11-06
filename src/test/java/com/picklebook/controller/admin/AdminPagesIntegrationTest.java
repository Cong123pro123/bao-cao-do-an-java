package com.picklebook.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.picklebook.entity.Court;
import com.picklebook.repository.BranchRepository;
import com.picklebook.repository.CourtRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class AdminPagesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CourtRepository courtRepository;

    @Test
    void flywayMigrationPreservesCourtsAndAssignsBranches() {
        assertEquals(2, branchRepository.count());
        assertEquals(1L, courtRepository.findById(1L).orElseThrow().getBranch().getId());
        assertEquals(2L, courtRepository.findById(3L).orElseThrow().getBranch().getId());
    }

    @Test
    void adminDashboardAndManagementPagesRender() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
        mockMvc.perform(get("/admin/bookings"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/bookings"));
        mockMvc.perform(get("/admin/courts"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/courts"));
        mockMvc.perform(get("/admin/courts/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/court-form"));
        mockMvc.perform(get("/admin/courts/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/court-form"));
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"));
        mockMvc.perform(get("/admin/users/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"));
        mockMvc.perform(get("/admin/users/3"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-detail"));
        mockMvc.perform(get("/admin/timeline"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/timeline"));
        mockMvc.perform(get("/admin/recurring"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/recurring-contracts"));
        mockMvc.perform(get("/admin/blocked-times"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/blocked-times"));
        mockMvc.perform(get("/admin/blocked-times/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/blocked-time-form"));
        mockMvc.perform(get("/admin/services"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/services"));
        mockMvc.perform(get("/admin/services/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/service-form"));
        mockMvc.perform(get("/admin/pricing"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/pricing-rules"));
        mockMvc.perform(get("/admin/pricing/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/pricing-rule-form"));
        mockMvc.perform(get("/admin/bookings/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    void seededCourtAvailabilityUsesSixAmOpeningTime() throws Exception {
        LocalDate date = LocalDate.now().plusDays(2);
        mockMvc.perform(get("/api/courts/1/slots").param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startAt").value(date + "T06:00:00"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCanManageBookingsButCannotManageUsers() throws Exception {
        mockMvc.perform(get("/admin/bookings"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/courts"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateCourtWithWeeklyOpeningHours() throws Exception {
        String name = "Sân kiểm thử " + System.nanoTime();
        var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/courts")
                .param("name", name)
                .param("branchId", "1")
                .param("courtType", "OUTDOOR")
                .param("status", "ACTIVE")
                .param("basePrice", "180000")
                .param("slotMinutes", "60")
                .param("bufferMinutes", "0")
                .param("minNoticeMinutes", "60")
                .param("maxAdvanceDays", "30")
                .param("maxSlotsPerBooking", "4")
                .param("amenities", "Bãi đỗ xe, Thuê vợt")
                .param("sortOrder", "10");
        for (int day = 1; day <= 7; day++) {
            request.param("schedules[" + (day - 1) + "].dayOfWeek", Integer.toString(day));
            request.param("schedules[" + (day - 1) + "].openTime", "06:00");
            request.param("schedules[" + (day - 1) + "].closeTime", "22:00");
        }

        mockMvc.perform(request.with(csrf()))
                .andExpect(status().is3xxRedirection());

        Court created = courtRepository.findAll().stream()
                .filter(court -> court.getName().equals(name))
                .findFirst()
                .orElseThrow();
        Court withSchedules = courtRepository.findWithDetailsById(created.getId()).orElseThrow();
        assertEquals(7, withSchedules.getSchedules().size());
        assertEquals(180000, withSchedules.getBasePrice().intValue());
        courtRepository.delete(withSchedules);
    }
}
